// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.io.{ByteArrayInputStream, File}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import cats.effect.{ContextShift, IO, Timer}
import sttp.client3._
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, OptionValues}
import org.scalatest.EitherValues.convertEitherToValuable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class EchoServlet extends HttpServlet {
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    resp.setContentType("text/plain")
    Option(req.getHeader("X-Content-Length"))
      .orElse(Option(req.getHeader("Content-Length")))
      .foreach(resp.addHeader(GzipSttpBackendTest.originalLength, _))
    val _ = IOUtils.copy(req.getInputStream, resp.getOutputStream)
    ()
  }
}

@SuppressWarnings(Array("org.wartremover.warts.GlobalExecutionContext", "org.wartremover.warts.NonUnitStatements"))
class GzipSttpBackendTest extends AnyFlatSpec with OptionValues with BeforeAndAfter {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val port: Int = 50000 + (java.lang.Math.random() * 1000).toInt

  val server: Server = new Server(port)
  private val gzipHandler = new GzipHandler()
  gzipHandler.addIncludedMethods("POST")
  gzipHandler.setInflateBufferSize(8096)
  private val servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS)
  servletHandler.insertHandler(gzipHandler)
  servletHandler.setInitParameter("gzip", "true")
  servletHandler.addServlet(classOf[EchoServlet], "/")

  before {
    server.setHandler(servletHandler)
    server.start()
  }

  after {
    server.stop()
  }

  private val longTestString = "aaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb" * 50

  implicit val sttpBackend: SttpBackend[IO, Any] =
    new GzipSttpBackend[IO, Any](
      AsyncHttpClientCatsBackend[IO]().unsafeRunSync()
    )

  "GzipSttpBackend" should "not compress small string bodies" in {
    val smallTestString = "abcd"
    val request = basicRequest
      .body(smallTestString)
      .post(uri"http://localhost:$port/string")
    val r = request.send(sttpBackend).unsafeRunSync()
    // Very small strings will be larger when compressed.
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt === smallTestString.getBytes(StandardCharsets.UTF_8).length
    )
    assert(r.body.value contains smallTestString)
  }

  it should "compress larger string bodies" in {
    val request = basicRequest
      .body(longTestString)
      .post(uri"http://localhost:$port/string")
    val r = request.send(sttpBackend).unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes(StandardCharsets.UTF_8).length
    )
    assert(r.body.value contains longTestString)
  }

  it should "compress byte array bodies" in {
    val request = basicRequest
      .body(longTestString.getBytes(StandardCharsets.UTF_8))
      .post(uri"http://localhost:$port/bytearray")
    val r = request.send(sttpBackend).unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes(StandardCharsets.UTF_8).length
    )
    assert(r.body.value contains longTestString)
  }

  it should "compress byte buffer bodies" in {
    val request = basicRequest
      .body(ByteBuffer.wrap(longTestString.getBytes(StandardCharsets.UTF_8)))
      .post(uri"http://localhost:$port/bytebuffer")
    val r = request.send(sttpBackend).unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes(StandardCharsets.UTF_8).length
    )
    assert(r.body.value contains longTestString)
  }

  it should "compress input stream bodies" in {
    val request = basicRequest
      .body(new ByteArrayInputStream(longTestString.getBytes(StandardCharsets.UTF_8)))
      .post(uri"http://localhost:$port/inputstream")
    val r = request.send(sttpBackend).unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes(StandardCharsets.UTF_8).length
    )
    assert(r.body.value contains longTestString)
  }

  it should "not interfere with multipart bodies" in {
    val request = basicRequest
      .multipartBody(
        multipart("string", "abc"),
        multipart("inputstream", new ByteArrayInputStream("defg".getBytes(StandardCharsets.UTF_8)))
      )
      .post(uri"http://localhost:$port/multipart")
    val r = request.send(sttpBackend).unsafeRunSync()
    assert(r.body.value contains "abc")
    assert(r.body.value contains "defg")
  }

  it should "not compress file input bodies" in {
    // TODO: Except, maybe we should.
    val request = basicRequest
      .body(new File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt"))
      .post(uri"http://localhost:$port/file")
    val r = request.send(sttpBackend).unsafeRunSync()
    val source = Source.fromFile(
      Paths.get("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt").toFile
    )(scala.io.Codec.UTF8)
    try {
      assert(r.body.value contains source.mkString)
    } finally {
      source.close()
    }
  }

  it should "not compress an already compressed request" in {
    val compressedBody = GzipSttpBackend.compress(longTestString.getBytes("utf-8"))
    val request = basicRequest
      .body(compressedBody)
      .header("Content-encoding", "gzip")
      .post(uri"http://localhost:$port/donotcompress")
    val r = request.send(sttpBackend).unsafeRunSync()
    assert(r.header(GzipSttpBackendTest.originalLength).value.toInt === compressedBody.length)
    assert(r.body.value contains longTestString)

    val request2 = basicRequest
      .body(compressedBody)
      .response(asByteArray)
      .header("Content-type", "text/gzip")
      .post(uri"http://localhost:$port/donotcompress")
    val r2 = request2.send(sttpBackend).unsafeRunSync()

    val gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(r2.body.value))
    try {
      val uncompressedBody = new String(IOUtils.toByteArray(gzipInputStream), StandardCharsets.UTF_8)
      assert(r2.header(GzipSttpBackendTest.originalLength).value.toInt === compressedBody.length)
      assert(uncompressedBody === longTestString)
    } finally {
      gzipInputStream.close()
    }
  }
}

object GzipSttpBackendTest {
  val originalLength = "X-original-length"
}
