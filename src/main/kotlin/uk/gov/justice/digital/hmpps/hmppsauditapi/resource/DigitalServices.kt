package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.util.UUID

@RestController
@RequestMapping("/audit/query", produces = [MediaType.APPLICATION_JSON_VALUE])
class DigitalServices(
  private val auditService: AuditService,
  private val auditQueueService: AuditQueueService,
  private val telemetryClient: TelemetryClient,
) {

  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Trigger query to get audit events for staff member",
    description = "Trigger query to get audit events given who, or subject ID and subject type, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @PostMapping
  fun startQuery(
    @RequestBody @Valid auditFilterDto: DigitalServicesQueryRequest,
  ): DigitalServicesQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      auditFilterDto,
    )
    val authorisedServices = getAuthorisedServices()
    telemetryClient.trackEvent("mohamad-test", mapOf(Pair("authorisedServices", authorisedServices.toString()))) // TODO remove
    return auditService.triggerQuery(auditFilterDto, authorisedServices)
  }

  fun getAuthorisedServices(): List<String> {
    val authentication = SecurityContextHolder.getContext().authentication
    return authentication?.authorities
      ?.map(GrantedAuthority::getAuthority)
      ?.filter { it.startsWith("ROLE_QUERY_AUDIT__") }
      ?.map { it.removePrefix("ROLE_QUERY_AUDIT__").lowercase().replace('_', '-') }
      ?: emptyList()
  }

  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Get audit events for staff member",
    description = "Get audit events given who, or subject ID and subject type, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @GetMapping("/{queryExecutionId}")
  fun getQueryResults(
    @PathVariable queryExecutionId: UUID,
  ): DigitalServicesQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      queryExecutionId,
    )
    return auditService.getQueryResults(queryExecutionId.toString())
  }
}
