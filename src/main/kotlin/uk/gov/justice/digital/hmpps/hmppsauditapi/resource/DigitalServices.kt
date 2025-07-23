package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AthenaPartitionRepairService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.util.UUID

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class DigitalServices(
  private val auditService: AuditService,
  private val auditQueueService: AuditQueueService,
  private val athenaPartitionRepairService: AthenaPartitionRepairService,
) {

  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Trigger query to get audit events for staff member",
    description = "Trigger query to get audit events given who, or subject ID and subject type, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @PostMapping("/query")
  fun startQuery(
    @RequestBody @Valid auditFilterDto: DigitalServicesQueryRequest,
  ): AthenaQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      auditFilterDto,
    )
    return auditService.triggerQuery(auditFilterDto, AuditEventType.STAFF)
  }

  @PreAuthorize("hasRole('ROLE_AUDIT') or hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  @Operation(
    summary = "Get audit events for staff member",
    description = "Get audit events given who, or subject ID and subject type, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
  )
  @StandardApiResponses
  @GetMapping("/query/{queryExecutionId}")
  fun getQueryResults(
    @PathVariable queryExecutionId: UUID,
  ): AthenaQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      queryExecutionId,
    )
    return auditService.getQueryResults(queryExecutionId.toString())
  }

  @PreAuthorize("hasRole('ROLE_AUDIT') or hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  @PostMapping("/query/repair-partitions") // TODO test
  fun triggerRepairPartitions(): AthenaQueryResponse = athenaPartitionRepairService.triggerRepairPartitions(AuditEventType.STAFF)

  @PreAuthorize("hasRole('ROLE_AUDIT') or hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  @GetMapping("/query/repair-partitions/{queryExecutionId}") // TODO test
  fun repairPartitions(
    @PathVariable queryExecutionId: UUID,
  ): AthenaQueryResponse = athenaPartitionRepairService.getRepairPartitionsResult(queryExecutionId)
}
