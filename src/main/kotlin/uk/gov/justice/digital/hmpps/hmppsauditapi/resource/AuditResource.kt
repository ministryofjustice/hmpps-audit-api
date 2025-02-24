package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesAuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesAuditQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.io.IOException
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class AuditResource(
  private val auditService: AuditService,
  private val auditQueueService: AuditQueueService,
) {
  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Get all audit events",
    description = "Get all audit events, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @GetMapping("")
  fun findAll(): List<AuditDto> {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_ALL.name,
      "",
    )
    return auditService.findAll()
  }

  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')") // TODO which roles?
  @Operation(
    summary = "Trigger query to get audit events for staff member",
    description = "Trigger query to get audit events given who, or subject ID and subject type, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @PostMapping("/query")
  fun startQueryForAuditEventsForStaffMember(
    pageable: Pageable = Pageable.unpaged(),
    @RequestBody @Valid
    auditFilterDto: DigitalServicesAuditFilterDto,
  ): DigitalServicesAuditQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      auditFilterDto,
    )
    return auditService.triggerQuery(auditFilterDto)
  }

  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')") // TODO which roles?
  @Operation(
    summary = "Get audit events for staff member",
    description = "Get audit events given who, or subject ID and subject type, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @PostMapping("/query")
  fun findAuditEventsForStaffMember(
    pageable: Pageable = Pageable.unpaged(),
    @RequestBody @Valid
    auditFilterDto: DigitalServicesAuditFilterDto,
  ): DigitalServicesAuditQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      auditFilterDto,
    )
    return auditService.triggerQuery(auditFilterDto)
  }

  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @Operation(
    summary = "Get paged audit events",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @PostMapping("/paged")
  @StandardApiResponses
  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
    @RequestBody @Valid
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto> {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_ALL_PAGED.name,
      auditFilterDto,
    )
    return auditService.findPage(pageable, auditFilterDto)
  }

  @Deprecated("Audit events should be sent via audit queue")
  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_write')")
  @PostMapping("")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(hidden = true)
  fun insertAuditEvent(
    @RequestHeader(value = "traceparent", required = false) traceParent: String?,
    @RequestBody auditEvent: AuditEvent,
  ) {
    val cleansedAuditEvent = auditEvent.copy(
      operationId = auditEvent.operationId ?: traceParent?.traceId(),
      details = auditEvent.details?.jsonString(),
    )
    auditQueueService.sendAuditEvent(cleansedAuditEvent)
  }

  private fun String.traceId(): String? {
    val traceParentElements = split("-")
    return if (traceParentElements.size == 4) traceParentElements[1] else null
  }

  private fun String.jsonString(): String? = try {
    jacksonObjectMapper().readTree(trim())
    ifBlank { null }
  } catch (e: IOException) {
    "{\"details\":\"$this\"}"
  }
}

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

enum class AuditType {
  AUDIT_GET_ALL,
  AUDIT_GET_ALL_PAGED,
  AUDIT_GET_BY_SERVICE,
  AUDIT_GET_BY_USER,
  AUDIT_GET_BY_DATE,
  AUDIT_GET_BY_DATE_TIME_BETWEEN,
}
