package com.github.nebtrx.microexample.producer

import cats.effect._
import cats.mtl.ApplicativeAsk
import com.github.nebtrx.microexample.common.models.Tweet
import com.github.nebtrx.microexample.producer.config.AppConfig
import fs2.Stream
import fs2.text.lines
import io.circe.Json
import io.circe.fs2._
import io.circe.generic.auto._
import jawn.RawFacade
import jawnfs2._
import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1

import scala.concurrent.ExecutionContext.global

class TWStream[F[_]](
    implicit F: ConcurrentEffect[F],
    CS: ContextShift[F],
    A: ApplicativeAsk[F, AppConfig]) {
  // jawn-fs2 needs to know what JSON AST you want
  implicit val facade: RawFacade[Json] = io.circe.jawn.CirceSupportParser.facade

  /* These values are created by a Twitter developer web app.
   * OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  def signRequest(
      consumerKey: String,
      consumerSecret: String,
      accessToken: String,
      accessSecret: String)(req: Request[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  def stream: Stream[F, Tweet] = {
    val stream = for {
      client <- BlazeClientBuilder(global).stream
      config <- Stream.eval(A.ask).map(_.twitterConfig)
      consumerKey = config.consumerKey
      consumerSecret = config.consumerSecret
      accessToken = config.accessToken
      accessSecret = config.accessSecret
      statusUri = config.endpoint

      req = Request[F](Method.GET, statusUri)
      sr <- Stream.eval(signRequest(consumerKey, consumerSecret, accessToken, accessSecret)(req))
      st <- client.stream(sr).flatMap(_.body.chunks.parseJsonStream)
    } yield st

    stream
      .map(_.spaces2)
      .through(lines)
      .through(stringStreamParser)
      .through(decoder[F, Tweet])
  }
}

object TWStream {
  def apply[F[_]](
      implicit F: ConcurrentEffect[F],
      CS: ContextShift[F],
      A: ApplicativeAsk[F, AppConfig]): TWStream[F] =
    new TWStream[F]()
}
