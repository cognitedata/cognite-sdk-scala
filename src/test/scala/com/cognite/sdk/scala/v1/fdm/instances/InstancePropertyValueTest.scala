package com.cognite.sdk.scala.v1.fdm.instances

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import scala.util.Try

@SuppressWarnings(
  Array(
    "org.wartremover.warts.JavaSerializable",
    "org.wartremover.warts.Serializable",
    "org.wartremover.warts.NonUnitStatements",
    "org.wartremover.warts.Product",
    "org.wartremover.warts.AnyVal",
    "org.wartremover.warts.AsInstanceOf",
    "org.wartremover.warts.Null"
  )
)
class InstancePropertyValueTest extends AnyWordSpec with Matchers {

  "InstanceFilterValue" should {

    "format UTC timestamps as YYYY-MM-DDTHH:MM:SS[.millis][Z|time zone]" in {
      val formatter = InstancePropertyValue.Timestamp.formatter
      val timestamp = LocalDateTime
        .of(2023, 1, 1, 1, 1, 1, 1000000)
        .atZone(ZoneId.of("UTC"))
      val formattedStr = formatter.format(timestamp)

      formattedStr shouldBe "2023-01-01T01:01:01.001Z"
    }

    "format non UTC timestamps as YYYY-MM-DDTHH:MM:SS[.millis][Z|time zone]" in {
      val formatter = InstancePropertyValue.Timestamp.formatter
      val timestamp = LocalDateTime
        .of(2023, 1, 1, 1, 1, 1, 1000000)
        .atZone(ZoneId.of("Asia/Colombo"))
      val formattedStr = formatter.format(timestamp)
      formattedStr shouldBe "2023-01-01T01:01:01.001+05:30"
    }

    "pass UTC timestamps formatted as YYYY-MM-DDTHH:MM:SS[.millis][Z|time zone]" in {
      val formatter = InstancePropertyValue.Timestamp.formatter
      val formattedStr = "2023-01-01T01:01:01.001Z"
      val parsedTs = ZonedDateTime.parse(formattedStr, formatter)
      val expectedTs = LocalDateTime
        .of(2023, 1, 1, 1, 1, 1, 1000000)
        .atZone(ZoneId.of("UTC"))

      parsedTs.getZone shouldBe ZoneId.of("Z")
      parsedTs.toLocalDateTime shouldBe expectedTs.toLocalDateTime
      parsedTs.toInstant.toEpochMilli shouldBe expectedTs.toInstant.toEpochMilli
    }

    "pass non UTC timestamps formatted as YYYY-MM-DDTHH:MM:SS[.millis][Z|time zone]" in {
      val formatter = InstancePropertyValue.Timestamp.formatter
      val formattedStr = "2023-01-01T01:01:01.001+05:30"
      val parsedTs = ZonedDateTime.parse(formattedStr, formatter)
      val expectedTs = LocalDateTime
        .of(2023, 1, 1, 1, 1, 1, 1000000)
        .atZone(ZoneId.of("Asia/Colombo"))

      parsedTs.getZone.toString shouldBe ZoneOffset.of("+05:30").toString
      parsedTs.toLocalDateTime shouldBe expectedTs.toLocalDateTime
      parsedTs.toInstant.toEpochMilli shouldBe expectedTs.toInstant.toEpochMilli
    }

    "pass timestamps with/without millis" in {
      val formatter = InstancePropertyValue.Timestamp.formatter
      val timestamps = Vector(
        "2023-01-01T01:01:01Z",
        "2023-01-01T01:01:01+01:00",
        "2023-01-01T01:01:01.001Z",
        "2023-01-01T01:01:01.001+01:00"
      )
      val parsedTimestamps = timestamps.flatMap(t => Try(ZonedDateTime.parse(t)).toOption)
      val reformattedTimestamps = parsedTimestamps.flatMap(ts => Try(ts.format(formatter)).toOption)

      reformattedTimestamps shouldBe Vector(
        "2023-01-01T01:01:01.000Z",
        "2023-01-01T01:01:01.000+01:00",
        "2023-01-01T01:01:01.001Z",
        "2023-01-01T01:01:01.001+01:00"
      )
    }
  }
}
