package com.cognite.sdk.scala.common

import com.softwaremill.sttp.{MonadError, Request, Response, SttpBackend}

case class ClientCredentialsResponse(

)

class OAuth2ClientCredentialsSttpBackend[R[_], -S](
    delegate: SttpBackend[R, S],
    auth: ClientCredentialsAuth
) extends SttpBackend[R, S] {

  override def send[T](request: Request[T, S]): R[Response[T]] =

  override def close(): Unit = ???

  override def responseMonad: MonadError[R] = delegate.responseMonad
}