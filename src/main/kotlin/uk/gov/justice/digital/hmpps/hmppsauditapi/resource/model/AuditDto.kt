package uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.time.Instant
import java.util.UUID

@JsonInclude(NON_NULL)
@Schema(description = "Audit Event Information")
data class AuditDto(
  @Schema(description = "Audit Event Id", example = "0f21b9e0-d153-42c6-a9ab-d583fe590987")
  val id: UUID,
  @Schema(description = "Detailed description of the Event", example = "COURT_REGISTER_BUILDING_UPDATE")
  val what: String,
  @Schema(description = "When the Event occurred", example = "2021-04-01T15:15:30Z")
  val `when`: Instant,
  @Schema(description = "The App Insights operation Id for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val operationId: String?,
  @Schema(description = "The subject ID for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val subjectId: String? = null,
  @Schema(description = "The subject type for the Event", example = "PERSON")
  val subjectType: String? = null,
  @Schema(description = "The correlation ID for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val correlationId: String? = null,
  @Schema(description = "Who initiated the Event", example = "fred.smith@myemail.com")
  val who: String?,
  @Schema(description = "Which service the Event relates to", example = "court-register")
  val service: String?,
  @Schema(
    description = "Additional information",
    example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}",
  )
  val details: String?,
) {
  constructor(auditEvent: AuditEvent) : this(
    auditEvent.id!!,
    auditEvent.what,
    auditEvent.`when`,
    auditEvent.operationId,
    auditEvent.subjectId,
    auditEvent.subjectType,
    auditEvent.correlationId,
    auditEvent.who,
    auditEvent.service,
    auditEvent.details,
  )
}
