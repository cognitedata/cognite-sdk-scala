package com.cognite.sdk.scala.common

import cats.effect.Async

final case class MockK8sServiceToken(success: Boolean = true) extends K8sServiceTokenProvider {
  def getKubernetesJwt[F[_]](implicit F: Async[F]): F[String] =
    if (success) {
      F.pure("kubernetesServiceToken")
    } else {
      F.raiseError(new SdkException(s"Failed to get service token"))
    }
}
