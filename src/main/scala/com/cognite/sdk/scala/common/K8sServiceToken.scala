package com.cognite.sdk.scala.common

import cats.effect.Async

import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import scala.util.Try

trait K8sServiceTokenProvider {
  def getKubernetesJwt[F[_]](implicit F: Async[F]): F[String]
}

object K8sServiceToken extends K8sServiceTokenProvider {
  def getKubernetesJwt[F[_]](implicit F: Async[F]): F[String] = {
    val serviceAccountTokenPath = "/var/run/secrets/tokens/cdf_token"
    Try(
      new String(Files.readAllBytes(Paths.get(serviceAccountTokenPath)), Charset.defaultCharset)
    ).toEither match {
      case Right(token) => F.pure(token)
      case Left(err) =>
        F.raiseError(
          new SdkException(s"Failed to get service token because ${err.getMessage}")
        )
    }
  }
}
