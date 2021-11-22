package sttp.client3.jsoniter_scala

import com.github.plokhotnyuk.jsoniter_scala.core.{
  JsonReaderException,
  JsonValueCodec,
  readFromString,
  writeToString
}
import sttp.client3._
import sttp.client3.internal.Utf8
import sttp.client3.json._
import sttp.model.MediaType

trait SttpJsoniterScalaApi {
  implicit def jsoniterScalaBodySerializer[B](
      implicit encoder: JsonValueCodec[B]
  ): BodySerializer[B] =
    b => StringBody(writeToString(b), Utf8, MediaType.ApplicationJson)

  /** If the response is successful (2xx), tries to deserialize the body from a string into JSON.
    * Returns:
    *   - `Right(b)` if the parsing was successful
    *   - `Left(HttpError(String))` if the response code was other than 2xx (deserialization is not
    *     attempted)
    *   - `Left(DeserializationException)` if there's an error during deserialization
    */
  def asJson[B: JsonValueCodec: IsOption]
      : ResponseAs[Either[ResponseException[String, JsonReaderException], B], Any] =
    asString.mapWithMetadata(ResponseAs.deserializeRightWithError(deserializeJson)).showAsJson

  /** Tries to deserialize the body from a string into JSON, regardless of the response code.
    * Returns:
    *   - `Right(b)` if the parsing was successful
    *   - `Left(DeserializationException)` if there's an error during deserialization
    */
  def asJsonAlways[B: JsonValueCodec: IsOption]
      : ResponseAs[Either[DeserializationException[JsonReaderException], B], Any] =
    asStringAlways.map(ResponseAs.deserializeWithError(deserializeJson)).showAsJsonAlways

  /** Tries to deserialize the body from a string into JSON, using different deserializers depending
    * on the status code. Returns:
    *   - `Right(B)` if the response was 2xx and parsing was successful
    *   - `Left(HttpError(E))` if the response was other than 2xx and parsing was successful
    *   - `Left(DeserializationException)` if there's an error during deserialization
    */
  def asJsonEither[E: JsonValueCodec: IsOption, B: JsonValueCodec: IsOption]
      : ResponseAs[Either[ResponseException[E, JsonReaderException], B], Any] =
    asJson[B].mapLeft {
      case HttpError(e, code) =>
        deserializeJson[E].apply(e).fold(DeserializationException(e, _), HttpError(_, code))
      case de @ DeserializationException(_, _) => de
    }.showAsJsonEither

  def deserializeJson[B: JsonValueCodec: IsOption]: String => Either[JsonReaderException, B] =
    JsonInput.sanitize[B].andThen { s =>
      try Right(readFromString[B](s))
      catch {
        case e: JsonReaderException => Left(e)
      }
    }
}
