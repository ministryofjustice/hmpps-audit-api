package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditType
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.PrisonerAuditService
import java.io.IOException
import java.util.UUID

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerAuditResource(
  private val prisonerAuditService: PrisonerAuditService,
  private val auditQueueService: AuditQueueService,
) {

  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT')")
  @Operation(
    summary = "Get paged prisoner audit events",
    security = [SecurityRequirement(name = "ROLE_PRISONER_AUDIT")],
  )
  @PostMapping("/prisoner/paged")
  @StandardApiResponses
  fun findPage(
    pageable: Pageable = Pageable.ofSize(100),
    @RequestBody @Valid
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto> {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_ALL_PAGED.name,
      auditFilterDto,
    )
    return prisonerAuditService.findPage(pageable, auditFilterDto)
  }

  @Hidden
  @Deprecated("Prisoner Audit events should be sent via audit queue")
  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT') and hasAuthority('SCOPE_write')")
  @PostMapping("/prisoner")
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
    auditQueueService.sendPrisonerAuditEvent(cleansedAuditEvent)
  }

  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Trigger query to get audit events for Prisoner",
    description = "Trigger query to get audit events given who, or subject ID and subject type, role required is ROLE_PRISONER_AUDIT",
    security = [SecurityRequirement(name = "ROLE_PRISONER_AUDIT")],
  )
  @StandardApiResponses
  @PostMapping("/prisoner/query")
  fun startQuery(
    @RequestBody @Valid auditFilterDto: AuditQueryRequest,
  ): AuditQueryResponse {
    if (auditFilterDto.auditEventType != AuditEventType.PRISONER) {
      throw EventTypeException("Incorrect event type used")
    }
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      auditFilterDto,
    )
    return prisonerAuditService.triggerQuery(auditFilterDto, AuditEventType.PRISONER)
  }

  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT')")
  @Operation(
    summary = "Get audit events for prisoner",
    description = "Get prisoner audit events given who, or subject ID and subject type, role required is ROLE_PRISONER_AUDIT",
    security = [SecurityRequirement(name = "ROLE_PRISONER_AUDIT")],
  )
  @StandardApiResponses
  @GetMapping("/prisoner/query/{queryExecutionId}")
  fun getQueryResults(
    @PathVariable queryExecutionId: UUID,
  ): AuditQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      queryExecutionId,
    )
    return prisonerAuditService.getQueryResults(queryExecutionId.toString())
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
