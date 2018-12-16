package com.github.nebtrx.microexample.webconsumer

import cats.effect.{Concurrent, Sync, Timer}
import cats.mtl.ApplicativeAsk
import com.github.gvolpe.fs2rabbit.interpreter.Fs2Rabbit
import com.github.gvolpe.fs2rabbit.json.Fs2JsonDecoder
import com.github.gvolpe.fs2rabbit.model.AmqpEnvelope
import com.github.gvolpe.fs2rabbit.util.StreamEval
import com.github.nebtrx.microexample.common.config.MessageQueueConfig
import com.github.nebtrx.microexample.common.models.Tweet
import fs2.{Pipe, Stream}
import io.chrisdavenport.log4cats.Logger
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

class RabbitConsumerService[F[_]: Sync](
    implicit timer: Timer[F],
    fs2rabbit: Fs2Rabbit[F],
    C: Concurrent[F],
    L: Logger[F],
    SE: StreamEval[F],
    A: ApplicativeAsk[F, MessageQueueConfig])
    extends Http4sDsl[F] {

  import io.circe.generic.auto._

  private val jsonDecoder = new Fs2JsonDecoder[F]

  import jsonDecoder.jsonDecode

  def logPipe: Pipe[F, AmqpEnvelope, AmqpEnvelope] = { streamMsg =>
    for {
      amqpMsg <- streamMsg
      _ <- SE.evalF[Unit](Logger[F].info(s"::::: Raw message received: $amqpMsg"))
    } yield amqpMsg
  }

  val rabbitStream: Stream[F, String] =
    RabbitConsumer[F].stream.flatten
      .through(logPipe)
      .through(jsonDecode[Tweet])
      .map(_._1.fold(_.toString, _.toString))

  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "stream" => Ok(rabbitStream)
    }
}

object RabbitConsumerService {
  def apply[F[_]: Sync](
      implicit timer: Timer[F],
      fs2rabbit: Fs2Rabbit[F],
      C: Concurrent[F],
      L: Logger[F],
      SE: StreamEval[F],
      A: ApplicativeAsk[F, MessageQueueConfig]): RabbitConsumerService[F] =
    new RabbitConsumerService[F]()
}
