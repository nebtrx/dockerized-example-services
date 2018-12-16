package com.github.nebtrx.microexample.producer

import cats.Applicative
import cats.effect._
import cats.mtl.ApplicativeAsk
import cats.syntax.functor._
import com.github.gvolpe.fs2rabbit.config.Fs2RabbitConfig
import com.github.gvolpe.fs2rabbit.interpreter.Fs2Rabbit
import com.github.gvolpe.fs2rabbit.model.{ExchangeName, QueueName, RoutingKey}
import com.github.gvolpe.fs2rabbit.resiliency.ResilientStream
import com.github.nebtrx.microexample.common.config.MessageQueueConfig
import com.github.nebtrx.microexample.producer.config.{AppConfig, TwitterConfig}
import org.http4s.Uri
import com.github.nebtrx.microexample.common.env._

object ProducerApp extends IOApp {
  // ENV VARS - PROGRAM FAILS TO START IF ANY VAR IS MISSING

  lazy val TwitterVars = new {
    val consumerKey: String = sys.env("TWITTER_CONSUMER_KEY")
    val consumerSecret: String = sys.env("TWITTER_CONSUMER_SECRET")
    val accessToken: String = sys.env("TWITTER_ACCESS_TOKEN")
    val accessSecret: String = sys.env("TWITTER_ACCESS_SECRET")
  }

  override def run(args: List[String]): IO[ExitCode] = {
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

    val twitterConfig = TwitterConfig(
      TwitterVars.consumerKey,
      TwitterVars.consumerSecret,
      TwitterVars.accessToken,
      TwitterVars.accessSecret,
      Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))

    implicit val fs2rabbit: Fs2Rabbit[IO] = Fs2Rabbit[IO](rabbitClientConfig)

    implicit val ask: ApplicativeAsk[IO, AppConfig] = new ApplicativeAsk[IO, AppConfig] {
      val applicative: Applicative[IO] = Applicative[IO]
      def ask: IO[AppConfig] = IO.pure(AppConfig(messageQueueConfig, twitterConfig))
      def reader[A](f: AppConfig => A): IO[A] = ask.map(f)
    }

    ResilientStream.run(RabbitProducer[IO].stream)
      .as(ExitCode.Success)

  }
}
