package com.cognite.sdk.scala.common

import java.io.{ByteArrayInputStream, File}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

import cats.effect.{ContextShift, IO, Timer}
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.scalatest.{BeforeAndAfter, OptionValues}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import org.scalatest.flatspec.AnyFlatSpec

class EchoServlet extends HttpServlet {
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    resp.setContentType("text/plain")
    Option(req.getHeader("X-Content-Length"))
      .orElse(Option(req.getHeader("Content-Length")))
      .foreach(resp.addHeader(GzipSttpBackendTest.originalLength, _))
    IOUtils.copy(req.getInputStream, resp.getOutputStream)
    ()
  }
}

class GzipSttpBackendTest extends AnyFlatSpec with OptionValues with BeforeAndAfter {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val port: Int = 50000 + (java.lang.Math.random() * 1000).toInt

  var server: Server = _
  val gzipHandler = new GzipHandler()
  gzipHandler.addIncludedMethods("POST")
  gzipHandler.setInflateBufferSize(8096)
  val servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS)
  servletHandler.setGzipHandler(gzipHandler)
  servletHandler.setInitParameter("gzip", "true")
  servletHandler.addServlet(classOf[EchoServlet], "/")

  before {
    server = new Server(port)
    server.setHandler(servletHandler)
    server.start()
  }

  after {
    server.stop()
  }

  private val longTestString = "aaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb" * 50

  implicit val sttpBackend: SttpBackend[IO, Nothing] =
    new GzipSttpBackend[IO, Nothing](
      AsyncHttpClientCatsBackend[IO]()
    )

  "GzipSttpBackend" should "not compress small string bodies" in {
    val smallTestString = "abcd"
    val request = sttp
      .body(smallTestString)
      .post(uri"http://localhost:$port/string")
    val r = request.send().unsafeRunSync()
    // Very small strings will be larger when compressed.
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt == smallTestString.getBytes().length
    )
    assert(r.unsafeBody == smallTestString)
  }

  it should "compress larger string bodies" in {
    val request = sttp
      .body(longTestString)
      .post(uri"http://localhost:$port/string")
    val r = request.send().unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes().length
    )
    assert(r.unsafeBody == longTestString)
  }

  it should "compress byte array bodies" in {
    val request = sttp
      .body(longTestString.getBytes)
      .post(uri"http://localhost:$port/bytearray")
    val r = request.send().unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes().length
    )
    assert(r.unsafeBody == longTestString)
  }

  it should "compress byte buffer bodies" in {
    val request = sttp
      .body(ByteBuffer.wrap(longTestString.getBytes))
      .post(uri"http://localhost:$port/bytebuffer")
    val r = request.send().unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes().length
    )
    assert(r.unsafeBody == longTestString)
  }

  it should "compress input stream bodies" in {
    val request = sttp
      .body(new ByteArrayInputStream(longTestString.getBytes))
      .post(uri"http://localhost:$port/inputstream")
    val r = request.send().unsafeRunSync()
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt < longTestString.getBytes().length
    )
    assert(r.unsafeBody == longTestString)
  }

  it should "not interfere with multipart bodies" in {
    val request = sttp
      .multipartBody(
        multipart("string", "abc"),
        multipart("inputstream", new ByteArrayInputStream("defg".getBytes))
      )
      .post(uri"http://localhost:$port/multipart")
    val r = request.send().unsafeRunSync()
    assert(r.unsafeBody.contains("abc"))
    assert(r.unsafeBody.contains("defg"))
  }

  it should "not compress file input bodies" in {
    // TODO: Except, maybe we should.
    val request = sttp
      .body(new File("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt"))
      .post(uri"http://localhost:$port/file")
    val r = request.send().unsafeRunSync()
    val source = Source.fromFile(
      Paths.get("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt").toFile
    )
    try {
      assert(r.unsafeBody == source.mkString)
    } finally {
      source.close()
    }
  }

  it should "not compress an already compressed request" in {
    val compressedBody = GzipSttpBackend.compress(longTestString.getBytes("utf-8"))
    val request = sttp
      .body(compressedBody)
      .header("Content-encoding", "gzip")
      .post(uri"http://localhost:$port/donotcompress")
    val r = request.send().unsafeRunSync()
    assert(r.header(GzipSttpBackendTest.originalLength).value.toInt == compressedBody.length)
    assert(r.unsafeBody == longTestString)

    val request2 = sttp
      .body(compressedBody)
      .response(asByteArray)
      .header("Content-type", "text/gzip")
      .post(uri"http://localhost:$port/donotcompress")
    val r2 = request2.send().unsafeRunSync()

    val gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(r2.unsafeBody))
    try {
      val uncompressedBody = new String(IOUtils.toByteArray(gzipInputStream), StandardCharsets.UTF_8)
      assert(r2.header(GzipSttpBackendTest.originalLength).value.toInt == compressedBody.length)
      assert(uncompressedBody == longTestString)
    } finally {
      gzipInputStream.close()
    }
  }
}

object GzipSttpBackendTest {
  val originalLength = "X-original-length"
}
