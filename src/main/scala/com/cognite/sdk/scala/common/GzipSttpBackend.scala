// Copyright 2020 Cognite AS
// SPDX-License-Identifier: Apache-2.0

package com.cognite.sdk.scala.common

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

import com.softwaremill.sttp._
import org.apache.commons.io.IOUtils

class GzipSttpBackend[R[_], S](delegate: SttpBackend[R, S], val minimumSize: Int = 1000)
    extends SttpBackend[R, S] {
  import GzipSttpBackend._

  private def isGzipped(keyAndValue: Tuple2[String, String]) =
    keyAndValue._1.equalsIgnoreCase(HeaderNames.ContentEncoding) &&
      keyAndValue._2.equalsIgnoreCase("gzip")

  @SuppressWarnings(Array("org.wartremover.warts.PlatformDefault"))
  private def isGzippedContent(keyAndValue: Tuple2[String, String]) =
    keyAndValue._1.equalsIgnoreCase(HeaderNames.ContentType) &&
      keyAndValue._2.toLowerCase.contains("/gzip")

  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  override def send[T](request: Request[T, S]): R[Response[T]] = {
    val headers = request.headers
    val newRequest = request.body match {
      case body: BasicRequestBody
          if !headers.exists(isGzipped) && !headers.exists(isGzippedContent) =>
        maybeCompressBody(body, minimumSize) match {
          // Note that if we add support for FileBody and don't return ByteArrayBody
          // in that case, we'll have to add another case for that here.
          case newBody @ ByteArrayBody(bytes, _) if newBody != request.body =>
            request
              .copy(
                body = newBody,
                headers = headers.filterNot(_._1.equalsIgnoreCase(HeaderNames.ContentLength))
              )
              .header(HeaderNames.ContentLength, bytes.length.toString)
              .header(HeaderNames.ContentEncoding, "gzip")
          case _ =>
            request
        }
      // TODO: Add support for streaming bodies.
      //       Will likely require us to be more strict about S, for example
      //       restricting it to fs2.Stream and using fs2.compress or fs2.compression.
      //  case StreamBody(s) =>
      // TODO: Add support for multipart bodies, though that seems difficult with sttp,
      //       since the multipart encoding happens after send(?).
      case _ => request
    }

    delegate.send(newRequest)
  }

  override def close(): Unit = delegate.close()

  override def responseMonad: MonadError[R] = delegate.responseMonad
}

object GzipSttpBackend {
  private val maximumBufferSize = 65536

  private[sdk] def compress(bytes: Array[Byte]): Array[Byte] = {
    val bufferSize = math.min(bytes.length, maximumBufferSize)
    val bos = new ByteArrayOutputStream(bufferSize)
    try {
      val gzip = new GZIPOutputStream(bos, bufferSize)
      try gzip.write(bytes)
      finally gzip.close()
    } finally bos.close()
    bos.toByteArray
  }

  // Compresses the body if it's larger than minimumSize.
  private[sdk] def maybeCompressBody(
      basicRequestBody: BasicRequestBody,
      minimumSize: Int
  ): BasicRequestBody =
    basicRequestBody match {
      // Comparing on string.length is an approximation, but a good enough one.
      case StringBody(string, encoding, defaultContentType) if string.length > minimumSize =>
        ByteArrayBody(compress(string.getBytes(Charset.forName(encoding))), defaultContentType)
      case ByteArrayBody(bytes, defaultContentType) if bytes.length > minimumSize =>
        ByteArrayBody(compress(bytes), defaultContentType)
      case ByteBufferBody(byteBuffer, defaultContentType)
          if byteBuffer.hasArray && byteBuffer.array().length > minimumSize =>
        ByteArrayBody(compress(byteBuffer.array()), defaultContentType)
      case InputStreamBody(inputStream, defaultContentType) =>
        // TODO: Optimize this using IOUtils.copy(input: InputStream, output: OutputStream)
        //       or something similar.
        ByteArrayBody(compress(IOUtils.toByteArray(inputStream)), defaultContentType)
      // TODO: Add support for FileBody.
      //  case FileBody(f, _)
      case _ =>
        basicRequestBody
    }
}
