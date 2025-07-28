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
class PrisonerDigitalServicesResource(
  private val auditService: AuditService,
  private val auditQueueService: AuditQueueService,
  private val athenaPartitionRepairService: AthenaPartitionRepairService,
) {

  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Trigger query to get audit events for prisoner",
    description = "Trigger query to get audit events given who, or subject ID and subject type, role required is ROLE_PRISONER_AUDIT",
    security = [SecurityRequirement(name = "ROLE_PRISONER_AUDIT")],
  )
  @StandardApiResponses
  @PostMapping("/prisoner/query")
  fun startPrisonerQuery(
    @RequestBody @Valid auditFilterDto: DigitalServicesQueryRequest,
  ): AthenaQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      auditFilterDto,
    )
    return auditService.triggerQuery(auditFilterDto, AuditEventType.PRISONER)
  }

  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT') and hasAuthority('SCOPE_read')")
  @Operation(
    summary = "Get audit events for prisoner",
    description = "Get audit events given who, or subject ID and subject type, role required is ROLE_PRISONER_AUDIT",
    security = [SecurityRequirement(name = "ROLE_PRISONER_AUDIT")],
  )
  @StandardApiResponses
  @GetMapping("/prisoner/query/{queryExecutionId}")
  fun getPrisonerQueryResults(
    @PathVariable queryExecutionId: UUID,
  ): AthenaQueryResponse {
    auditQueueService.sendAuditAuditEvent(
      AuditType.AUDIT_GET_BY_USER.name,
      queryExecutionId,
    )
    return auditService.getQueryResults(queryExecutionId.toString())
  }

  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT')")
  @PostMapping("/prisoner/repair-partitions") // TODO test
  fun triggerRepairPrisonerPartitions(): AthenaQueryResponse = athenaPartitionRepairService.triggerRepairPartitions(AuditEventType.PRISONER)

  // TODO test
  @PreAuthorize("hasRole('ROLE_PRISONER_AUDIT')")
  @GetMapping("/prisoner/repair-partitions/{queryExecutionId}") // TODO test
  fun getRepairPrisonerPartitionsQueryResults(
    @PathVariable queryExecutionId: UUID,
  ): AthenaQueryResponse = athenaPartitionRepairService.getRepairPartitionsResult(queryExecutionId)
}
