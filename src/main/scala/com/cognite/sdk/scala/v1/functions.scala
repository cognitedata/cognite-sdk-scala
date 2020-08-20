package com.cognite.sdk.scala.v1

import java.time.Instant
import com.cognite.sdk.scala.common._
import io.circe.Json

final case class FunctionError(
    message: Option[String] = None,
    trace: Option[String] = None
)

final case class Function(
    id: Option[Long] = None,
    name: String = "",
    fileId: Long = 0,
    owner: Option[String] = None,
    description: Option[String] = None,
    apiKey: Option[String] = None,
    secrets: Option[Map[String, String]] = None,
    createdTime: Option[Instant] = None,
    status: Option[String] = None,
    externalId: Option[String] = None,
    error: Option[FunctionError] = None
)

final case class FunctionCreate(
    name: String = "",
    fileId: Long = 0,
    owner: Option[String] = None,
    description: Option[String] = None,
    apiKey: Option[String] = None,
    secrets: Option[Map[String, String]] = None,
    externalId: Option[String] = None,
    error: Option[FunctionError] = None
) extends WithExternalId

final case class FunctionCall(
    id: Option[Long] = None,
    status: Option[String] = None,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    scheduleId: Option[Long] = None,
    functionId: Option[Long] = None
)

final case class FunctionCallLogEntry(
    timestamp: Option[Long] = None,
    message: Option[String] = None
)

final case class FunctionCallResponse(
    response: Option[Json] = None,
    functionId: Option[Long] = None,
    callId: Option[Long] = None
)

final case class FunctionCallFilter(
    scheduleId: Option[Long] = None,
    status: Option[String] = None,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None
)

final case class FunctionSchedule(
    id: Option[Long] = None,
    name: String = "",
    functionExternalId: String = "",
    createdTime: Option[Long] = None,
    description: Option[String] = None,
    cronExpression: Option[String] = None,
    data: Option[Json] = None
)

final case class FunctionScheduleCreate(
    name: String = "",
    functionExternalId: String = "",
    description: Option[String] = None,
    cronExpression: String = "",
    data: Option[Json] = None
)
