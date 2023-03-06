package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import javax.validation.constraints.Size
data class AuditFilterDto(
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

  @Schema(required = false, description = "The service which the audit events relates to", example = "offender-service")
  @field:Size(min = 2, max = 200)
  val service: String? = null,

  @Schema(required = false, description = "The user who initiated the audit event", example = "Joe.Bloggs")
  @field:Size(min = 2, max = 80)
  val who: String? = null,

  @Schema(
    required = false,
    description = "The function or process to filter audit events by",
    example = "COURT_REGISTER_BUILDING_UPDATE",
  )
  @field:Size(min = 2, max = 200)
  val what: String? = null,
)
