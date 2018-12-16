package com.github.nebtrx.microexample.webconsumer

import cats.effect.{Concurrent, Timer}
import cats.mtl.ApplicativeAsk
import com.github.gvolpe.fs2rabbit.config.declaration.DeclarationQueueConfig
import com.github.gvolpe.fs2rabbit.interpreter.Fs2Rabbit
import com.github.gvolpe.fs2rabbit.model._
import com.github.gvolpe.fs2rabbit.util.StreamEval
import com.github.nebtrx.microexample.common.config.MessageQueueConfig
import fs2.Stream

class RabbitConsumer[F[_]: Concurrent: Timer](
    implicit F: Fs2Rabbit[F],
    SE: StreamEval[F],
    A: ApplicativeAsk[F, MessageQueueConfig]) {

  def stream: Stream[F, StreamConsumer[F]] = F.createConnectionChannel flatMap { implicit channel =>
    for {
      config <- Stream.eval(A.ask)
      _ <- F.declareQueue(DeclarationQueueConfig.default(config.queueName))
      _ <- F.declareExchange(config.exchangeName, ExchangeType.Topic)
      _ <- F.bindQueue(config.queueName, config.exchangeName, config.routingKey)
      consumer <- F.createAutoAckConsumer(config.queueName)
    } yield consumer
  }
}

object RabbitConsumer {
  def apply[F[_]: Concurrent: Timer](
      implicit f: Fs2Rabbit[F],
      se: StreamEval[F],
      A: ApplicativeAsk[F, MessageQueueConfig]): RabbitConsumer[F] =
    new RabbitConsumer[F]
}
