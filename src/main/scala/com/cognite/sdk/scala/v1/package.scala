package com.cognite.sdk.scala

import java.util.concurrent.Executors

import scala.concurrent.duration._

import cats.{Comonad, Id}
import cats.effect.IO
import com.softwaremill.sttp.{HttpURLConnectionBackend, SttpBackend}
import com.cognite.sdk.scala.common.RetryingBackend

import scala.concurrent.ExecutionContext

package object v1 {
  implicit val executionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  implicit val sttpBackend: SttpBackend[Id, Nothing] =
    new RetryingBackend[Id, Nothing](
      HttpURLConnectionBackend(),
      initialRetryDelay = 100.millis,
      maxRetryDelay = 200.millis
    )

  // This is ugly and not great, but we need to do something hacky one
  // way or another to be able to support both IO and Id types.
  implicit val comonadIO: Comonad[IO] = new Comonad[IO] {
    override def extract[A](x: IO[A]): A = x.unsafeRunSync()

    override def coflatMap[A, B](fa: IO[A])(f: IO[A] => B): IO[B] = IO.pure(f(fa))

    override def map[A, B](fa: IO[A])(f: A => B): IO[B] = fa.map(f)
  }
}
