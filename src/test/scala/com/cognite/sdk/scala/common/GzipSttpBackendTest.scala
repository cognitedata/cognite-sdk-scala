package com.cognite.sdk.scala.common

import java.io.{ByteArrayInputStream, File}
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths}

import cats.effect.{ContextShift, IO, Timer}
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.scalatest.{BeforeAndAfter, FlatSpec, OptionValues}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class EchoServlet extends HttpServlet {
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    resp.setContentType("text/plain")
    Option(req.getHeader("X-Content-Length"))
      .orElse(Option(req.getHeader("Content-Length")))
      .foreach(resp.addHeader(GzipSttpBackendTest.originalLength, _))
    val receivedContent = req.getReader.lines().iterator().asScala.mkString("")
    resp.getWriter.print(receivedContent)
  }
}

class GzipSttpBackendTest extends FlatSpec with OptionValues with BeforeAndAfter {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  val port: Int = 50000 + (math.random * 1000).toInt

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

  private val longTestString = "aaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbb".repeat(50)

  implicit val sttpBackend: SttpBackend[IO, Nothing] =
    new GzipSttpBackend[IO, Nothing](
      AsyncHttpClientCatsBackend[IO]()
    )

  "GzipSttpBackend" should "not compress small string bodies" in {
    val testString1 = "abcd"
    val request = sttp
      .body(testString1)
      .post(uri"http://localhost:$port/string")
    val r = request.send().unsafeRunSync()
    // Very small strings will be larger when compressed.
    assert(
      r.header(GzipSttpBackendTest.originalLength).value.toInt == testString1.getBytes().length
    )
    assert(r.unsafeBody == testString1)
  }

  it should "compress larger string bodies" in {
    val request = sttp
      .body(longTestString)
      .post(uri"http://localhost:$port/string")
    val r = request.send().unsafeRunSync()
    // Very small strings will be larger when compressed.
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
    val hm = request.send().unsafeRunSync()
    assert(
      hm.unsafeBody == Files
        .readString(Paths.get("./src/test/scala/com/cognite/sdk/scala/v1/uploadTest.txt"))
    )
  }

  it should "not compress an already compressed request" in {
    val compressedBody = GzipSttpBackend.compress(longTestString.getBytes)
    val request = sttp
      .body(compressedBody)
      .header("Content-encoding", "gzip")
      .post(uri"http://localhost:$port/donotcompress")
    val r = request.send().unsafeRunSync()
    assert(r.header(GzipSttpBackendTest.originalLength).value.toInt == compressedBody.length)
    assert(r.unsafeBody == longTestString)
  }
}

object GzipSttpBackendTest {
  val originalLength = "X-original-length"
}
