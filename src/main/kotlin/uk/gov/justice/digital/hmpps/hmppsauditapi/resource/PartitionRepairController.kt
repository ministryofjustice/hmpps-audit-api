package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AthenaPartitionRepairService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditAthenaClient
import java.util.UUID

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class PartitionRepairController(
  private val athenaPartitionRepairService: AthenaPartitionRepairService,
  private val auditAthenaClient: AuditAthenaClient,
) {
  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @PostMapping("/{auditEventType}/partition-repair")
  fun triggerRepairPartitions(
    @PathVariable auditEventType: AuditEventType,
  ): AthenaQueryResponse = athenaPartitionRepairService.triggerRepairPartitions(auditEventType)

  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @GetMapping("/query/partition-repair/{queryExecutionId}")
  fun getRepairPartitionsResults(
    @PathVariable queryExecutionId: UUID,
  ): AthenaQueryResponse = auditAthenaClient.getQueryResults(queryExecutionId.toString())
}
