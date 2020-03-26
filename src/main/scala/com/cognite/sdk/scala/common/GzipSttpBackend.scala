package com.cognite.sdk.scala.common

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

import com.softwaremill.sttp._
import org.apache.commons.io.IOUtils

class GzipSttpBackend[R[_], S](delegate: SttpBackend[R, S], val minimumSize: Int = 1000)
    extends SttpBackend[R, S] {
  import GzipSttpBackend._

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  implicit final class AnyOps[A](self: A) {
    def ===(other: A): Boolean = self == other
  }

  private def isGzipped(keyAndValue: Tuple2[String, String]) =
    keyAndValue._1.equalsIgnoreCase(HeaderNames.ContentEncoding) &&
      keyAndValue._2.equalsIgnoreCase("gzip")

  private def isGzippedContent(keyAndValue: Tuple2[String, String]) =
    keyAndValue._1.equalsIgnoreCase(HeaderNames.ContentType) &&
      keyAndValue._2.toLowerCase.contains("/gzip")

  override def send[T](request: Request[T, S]): R[Response[T]] = {
    val (newBody, newContentLength) = request.body match {
      case body: BasicRequestBody
          if !request.headers.exists(isGzipped)
            && !request.headers.exists(isGzippedContent) =>
        compressBody(body, minimumSize)
      // TODO: Add support for streaming bodies.
      //       Will likely require us to be more strict about S, for example
      //       restricting it to fs2.Stream and using fs2.compress or fs2.compression.
      //  case StreamBody(s) =>
      // TODO: Add support for multipart bodies, though that seems difficult with sttp,
      //       since the multipart encoding happens after send(?).
      case _ => (request.body, None)
    }
    val newRequest = if (newBody === request.body) {
      request
    } else {
      request
        .copy(body = newBody)
        .header(HeaderNames.ContentEncoding, "gzip")
    }
    val newRequestWithContentLength = newContentLength match {
      case Some(contentLength) =>
        newRequest
          .copy(
            headers = newRequest.headers.filterNot(_._1.equalsIgnoreCase(HeaderNames.ContentLength))
          )
          .header(HeaderNames.ContentLength, contentLength.toString)
      case None => newRequest
    }
    delegate.send(newRequestWithContentLength)
  }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[R] = delegate.responseMonad
}

object GzipSttpBackend {
  val minimumBufferSize = 10000

  private[sdk] def compress(bytes: Array[Byte]): Array[Byte] = {
    val bos = new ByteArrayOutputStream(math.min(bytes.length, minimumBufferSize))
    try {
      val gzip = new GZIPOutputStream(bos)
      try {
        gzip.write(bytes)
      } finally {
        gzip.close()
      }
    } finally {
      bos.close()
    }
    bos.toByteArray
  }

  private[sdk] def compressBody(
      basicRequestBody: BasicRequestBody,
      minimumSize: Int
  ): (BasicRequestBody, Option[Int]) =
    basicRequestBody match {
      // Comparing on string.length is an approximation, but a good enough one.
      case StringBody(string, encoding, defaultContentType) if string.length > minimumSize =>
        val compressed = compress(string.getBytes(Charset.forName(encoding)))
        (ByteArrayBody(compressed, defaultContentType), Some(compressed.length))
      case ByteArrayBody(bytes, defaultContentType) if bytes.length > minimumSize =>
        val compressed = compress(bytes)
        (ByteArrayBody(compressed, defaultContentType), Some(compressed.length))
      case ByteBufferBody(byteBuffer, defaultContentType)
          if byteBuffer.hasArray && byteBuffer.array().length > minimumSize =>
        val compressed = compress(byteBuffer.array())
        (ByteArrayBody(compressed, defaultContentType), Some(compressed.length))
      case InputStreamBody(inputStream, defaultContentType) =>
        // TODO: Optimize this using IOUtils.copy(input: InputStream, output: OutputStream)
        //       or something similar.
        val compressed = compress(IOUtils.toByteArray(inputStream))
        (ByteArrayBody(compressed, defaultContentType), Some(compressed.length))
      // TODO: Add support for FileBody.
      //  case FileBody(f, _)
      case _ =>
        (basicRequestBody, None)
    }
}
