package com.github.nebtrx.microexample.common.config

import com.github.gvolpe.fs2rabbit.model.{ExchangeName, QueueName, RoutingKey}

case class MessageQueueConfig(queueName: QueueName,
                              exchangeName: ExchangeName,
                              routingKey: RoutingKey
                             )
