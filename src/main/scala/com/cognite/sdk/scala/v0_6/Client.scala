package com.cognite.sdk.scala.v0_6

import com.cognite.sdk.scala.common.{Auth, Login}
import com.softwaremill.sttp._
import io.circe.generic.auto._

final case class Data[A](data: A)
final case class ItemsWithCursor[A](items: Seq[A], nextCursor: Option[String] = None)
final case class Items[A](items: Seq[A])
final case class CdpApiErrorPayload(code: Int, message: String)
final case class Error[A](error: A)
final case class CdpApiException(url: Uri, code: Int, message: String)
    extends Throwable(s"Request to ${url.toString()} failed with status $code: $message")

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  // TODO: auth once here instead of passing Auth down
  //val sttp1 = sttp.auth(auth)

  val login = new Login()
  val assets = new Assets()
  val events = new Events()
  val timeSeries = new TimeSeriesResource()
}
