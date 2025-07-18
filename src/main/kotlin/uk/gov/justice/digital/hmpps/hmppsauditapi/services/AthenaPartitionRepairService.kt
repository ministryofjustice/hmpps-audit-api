package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Component
class AthenaPartitionRepairService(
  private val athenaClient: AthenaClient,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
) {

  fun repairPartitions(auditEventType: AuditEventType) {
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
    athenaClient.startQueryExecution(request)
  }

  @Scheduled(cron = "0 0 * * * *")
  fun triggerRepairPartitions() = repairPartitions(AuditEventType.STAFF)

  @Scheduled(cron = "0 15 * * * *")
  fun triggerPrisonerRepairPartitions() = repairPartitions(AuditEventType.PRISONER)
}
