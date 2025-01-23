package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.Instant

@ValidDigitalServicesAuditFilter
open class DigitalServicesAuditFilterDto(
  @Schema(
    required = false,
    description = "The start date and time to filter the audit events",
    example = "2020-07-01T15:15:30Z",
  )
  val startDateTime: Instant? = null,

  @Schema(
    required = false,
    description = "The end date and time to filter the audit events",
    example = "2020-07-01T15:15:30Z",
  )
  val endDateTime: Instant? = null,

  @Schema(required = false, description = "The user who initiated the audit event", example = "Joe.Bloggs")
  @field:Size(min = 2, max = 80)
  val who: String? = null,

  @Schema(required = false, description = "The subject ID of the audit event", example = "da8ea6d876c62e2f5264c94c7b50867r")
  @field:Size(min = 2, max = 200)
  val subjectId: String? = null,

  @Schema(required = false, description = "The subject type of the audit event", example = "PERSON")
  @field:Size(min = 2, max = 200)
  val subjectType: String? = null,
)
