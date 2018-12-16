package com.github.nebtrx.microexample.producer.config

import org.http4s.Uri

case class TwitterConfig(
    consumerKey: String,
    consumerSecret: String,
    accessToken: String,
    accessSecret: String,
    endpoint: Uri)
