package com.github.nebtrx.microexample.producer.config

import com.github.nebtrx.microexample.common.config.MessageQueueConfig

case class AppConfig(messageQueueConfig: MessageQueueConfig, twitterConfig: TwitterConfig)
