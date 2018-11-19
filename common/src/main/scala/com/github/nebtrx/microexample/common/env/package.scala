package com.github.nebtrx.microexample.common

package object env {
  object RabbitVars {
    val host = sys.env("RABBIT_HOST")
    val username = sys.env("RABBIT_USER")
    val password = sys.env("RABBIT_PASSWORD")
    val port = sys.env("RABBIT_PORT").toInt
    val ssl = false
  }

  object MessageVars {
    val queueName = sys.env("QUEUE_NAME")
    val exchangeName = sys.env("EXCHANGE_NAME")
    val routingKey = sys.env("ROUTING_KEY")
  }

}
