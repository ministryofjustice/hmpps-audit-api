package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionState.QUEUED
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import java.util.UUID

@Component
class AthenaPartitionRepairService(
  private val athenaClient: AthenaClient,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
  private val auditAthenaClient: AuditAthenaClient,
) {

  // TODO test
  fun triggerRepairPartitions(auditEventType: AuditEventType): AthenaQueryResponse {
    val athenaProperties = athenaPropertiesFactory.getProperties(auditEventType)
    val databaseName = athenaProperties.databaseName
    val tableName = athenaProperties.tableName
    val repairQuery = "MSCK REPAIR TABLE $databaseName.$tableName;"
    val request = StartQueryExecutionRequest.builder()
      .queryString(repairQuery)
      .queryExecutionContext { it.database(databaseName) }
      .workGroup(athenaProperties.workGroupName)
      .resultConfiguration { it.outputLocation(athenaProperties.outputLocation) }
      .build()
    val startQueryExecutionResponse = athenaClient.startQueryExecution(request)
    return AthenaQueryResponse(
      queryExecutionId = UUID.fromString(startQueryExecutionResponse.queryExecutionId()),
      queryState = QUEUED,
      authorisedServices = emptyList(),
    )
  }

  // TODO test
  fun getRepairPartitionsResult(queryExecutionId: UUID): AthenaQueryResponse = auditAthenaClient.getQueryResults(queryExecutionId.toString())

  @Scheduled(cron = "0 0 0 1 * *")
  fun triggerRepairPartitions() = triggerRepairPartitions(AuditEventType.STAFF)

  @Scheduled(cron = "0 0 0 1 * *")
  fun triggerPrisonerRepairPartitions() = triggerRepairPartitions(AuditEventType.PRISONER)
}
