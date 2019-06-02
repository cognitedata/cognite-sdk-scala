package com.cognite.sdk.scala.v1_0

import com.cognite.sdk.scala.common.{Auth, Extractor, Login}
import com.softwaremill.sttp._
import io.circe.generic.auto._

object ExtractorInstances {
  implicit val idExtractor: Extractor[Id] = new Extractor[Id] {
    override def extract[A](c: Id[A]): A = c
  }
}

final class Client[F[_]](implicit auth: Auth, sttpBackend: SttpBackend[F, _]) {
  // TODO: auth once here instead of passing Auth down
  //val sttp1 = sttp.auth(auth)

  val login = new Login()
  val assets = new Assets()
  val events = new Events()
  //val timeSeries = new TimeSeriesResource()
  val files = new Files()
}
