package com.cognite.sdk.scala.common

import com.cognite.auth.service.{Expected, ServiceTokenProvider}

case class MockK8sServiceTokenProvider(success: Boolean = true) extends ServiceTokenProvider {
  override def getIdToken(): Expected[String] =
    if (success) {
      Expected.of("kubernetesServiceToken")
    } else {
      throw new RuntimeException("Could not get Kubernetes JWT")
    }
}
