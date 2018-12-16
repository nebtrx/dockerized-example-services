package com.github.nebtrx.microexample.producer

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Timer}
import cats.mtl.ApplicativeAsk
import com.github.gvolpe.fs2rabbit.config.declaration.DeclarationQueueConfig
import com.github.gvolpe.fs2rabbit.interpreter.Fs2Rabbit
import com.github.gvolpe.fs2rabbit.json.Fs2JsonEncoder
import com.github.gvolpe.fs2rabbit.model.{
  AmqpMessage,
  AmqpProperties,
  ExchangeType,
  StreamPublisher
}
import com.github.gvolpe.fs2rabbit.util.StreamEval
import com.github.nebtrx.microexample.common.models.Tweet
import com.github.nebtrx.microexample.producer.config.AppConfig
import fs2.Stream

import scala.concurrent.duration._

class RabbitProducer[F[_]: Concurrent: Timer](
    implicit F: Fs2Rabbit[F],
    SE: StreamEval[F],
    CE: ConcurrentEffect[F],
    CS: ContextShift[F],
    A: ApplicativeAsk[F, AppConfig]) {
  private val jsonEncoder = new Fs2JsonEncoder[F]

  import jsonEncoder.jsonEncode

  def stream: Stream[F, Unit] = F.createConnectionChannel flatMap { implicit channel =>
    for {
      config <- Stream.eval(A.ask).map(_.messageQueueConfig)
      _ <- F.declareQueue(DeclarationQueueConfig.default(config.queueName))
      _ <- F.declareExchange(config.exchangeName, ExchangeType.Topic)
      _ <- F.bindQueue(config.queueName, config.exchangeName, config.routingKey)
      publisher <- F.createPublisher(config.exchangeName, config.routingKey)
      result <- flowTo(publisher)
    } yield result
  }

  private def flowTo(publisher: StreamPublisher[F]): Stream[F, Unit] = {
    import io.circe.generic.auto._

    val dataStream: Stream[F, AmqpMessage[Tweet]] = TWStream[F].stream
      .map(t => AmqpMessage(t, AmqpProperties.empty))

    val throttlerStream = Stream.awakeEvery[F](1.second)

    val finalStream: Stream[F, AmqpMessage[Tweet]] =
      dataStream.zipWith(throttlerStream)((v, _) => v)

    finalStream.covary[F] through jsonEncode[Tweet] to publisher
  }
}

object RabbitProducer {
  def apply[F[_]: Concurrent: Timer](
      implicit F: Fs2Rabbit[F],
      SE: StreamEval[F],
      CE: ConcurrentEffect[F],
      CS: ContextShift[F],
      A: ApplicativeAsk[F, AppConfig]): RabbitProducer[F] = new RabbitProducer[F]()
}
