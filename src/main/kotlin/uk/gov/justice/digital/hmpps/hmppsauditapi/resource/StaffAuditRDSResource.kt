package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
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
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditType
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditServiceFactory
import java.io.IOException

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class StaffAuditRDSResource(
  private val auditServiceFactory: AuditServiceFactory,
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
    return auditServiceFactory.getAuditService(AuditEventType.STAFF).findAll()
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
    return auditServiceFactory.getAuditService(AuditEventType.STAFF).findPage(pageable, auditFilterDto)
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
