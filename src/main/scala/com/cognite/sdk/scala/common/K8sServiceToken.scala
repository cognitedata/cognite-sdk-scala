package com.cognite.sdk.scala.common

import com.cognite.auth.service.{LocalServiceTokenProvider, RealServiceTokenProvider}

object K8sServiceToken {
  val tokenProvider =
    if (sys.env.contains("KUBERNETES_JWT_LOCATION")) {
      new LocalServiceTokenProvider(sys.env("KUBERNETES_JWT_LOCATION"))
    } else {
      new RealServiceTokenProvider()
    }
}
