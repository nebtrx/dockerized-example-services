package com.github.nebtrx.microexample.webconsumer

import cats.Applicative
import cats.effect._
import cats.implicits._
import cats.mtl.ApplicativeAsk
import com.github.gvolpe.fs2rabbit.config.Fs2RabbitConfig
import com.github.gvolpe.fs2rabbit.interpreter.Fs2Rabbit
import com.github.gvolpe.fs2rabbit.model.{ExchangeName, QueueName, RoutingKey}
import com.github.gvolpe.fs2rabbit.util.StreamEval
import com.github.nebtrx.microexample.common.config.MessageQueueConfig
import com.github.nebtrx.microexample.common.env.{MessageVars, RabbitVars}
import fs2.Stream
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

object WebConsumerApp extends IOApp {

  private val rabbitHost = "rabbitmq"
  private val rabbitUsername = Some("user")
  private val rabbitPassword = Some("bitnami")
  private val rabbitPort = 5672
  private val rabbitSsl = false

  def run(args: List[String]): IO[ExitCode] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val rabbitClientConfig: Fs2RabbitConfig = Fs2RabbitConfig(
      virtualHost = "/",
      host = RabbitVars.host,
      username = Some(RabbitVars.username),
      password = Some(RabbitVars.password),
      port = RabbitVars.port,
      ssl = false,
      sslContext = None,
      connectionTimeout = 3,
      requeueOnNack = false)

    val messageQueueConfig = MessageQueueConfig(
      queueName = QueueName(MessageVars.queueName),
      exchangeName = ExchangeName(MessageVars.exchangeName),
      routingKey = RoutingKey(MessageVars.routingKey)
    )

    implicit val timer: Timer[IO] = IO.timer(global)
    implicit val fs2rabbit: Fs2Rabbit[IO] = Fs2Rabbit[IO](rabbitClientConfig)

    implicit val ask: ApplicativeAsk[IO, MessageQueueConfig] = new ApplicativeAsk[IO, MessageQueueConfig] {
      val applicative: Applicative[IO] = Applicative[IO]
      def ask: IO[MessageQueueConfig] = IO.pure(messageQueueConfig)
      def reader[A](f: MessageQueueConfig => A): IO[A] = ask.map(f)
    }

    Server.run[IO](messageQueueConfig).compile.drain.as(ExitCode.Success)
  }
}

object Server {

  def run[F[_]: ConcurrentEffect](config: MessageQueueConfig)
                                 (implicit timer: Timer[F],
                                  fs2rabbit: Fs2Rabbit[F],
                                  SE: StreamEval[F],
                                  A: ApplicativeAsk[F, MessageQueueConfig]): Stream[F, ExitCode] =
    BlazeServerBuilder[F]
      .bindHttp(port = 8080, host = "0.0.0.0")
      .withHttpApp(RabbitConsumerService[F].routes.orNotFound)
      .serve

}
