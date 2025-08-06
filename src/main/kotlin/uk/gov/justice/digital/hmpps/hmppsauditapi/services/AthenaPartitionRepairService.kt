package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionState.QUEUED
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Component
class AthenaPartitionRepairService(
  private val athenaClient: AthenaClient,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
) {

  fun triggerRepairPartitions(auditEventType: AuditEventType): AthenaQueryResponse {
    val athenaProperties = athenaPropertiesFactory.getProperties(auditEventType)
    val repairQuery = "MSCK REPAIR TABLE ${athenaProperties.databaseName}.${athenaProperties.tableName};"
    return startAthenaQuery(repairQuery, athenaProperties)
  }

  fun triggerRepairForUserToday(auditEventType: AuditEventType, who: String): AthenaQueryResponse { // TODO test
    val today = LocalDate.now(ZoneId.systemDefault())
    val athenaProperties = athenaPropertiesFactory.getProperties(auditEventType)

    val year = today.year
    val month = today.monthValue
    val day = today.dayOfMonth

    val partitionS3Path = "s3://${athenaProperties.s3BucketName}/year=$year/month=$month/day=$day/user=$who/"

    val alterQuery = """
        ALTER TABLE ${athenaProperties.databaseName}.${athenaProperties.tableName}
        ADD IF NOT EXISTS PARTITION (year=$year, month=$month, day=$day, user='$who')
        LOCATION '$partitionS3Path'
    """.trimIndent()

    return startAthenaQuery(alterQuery, athenaProperties)
  }

  // TODO move into athena client?
  private fun startAthenaQuery(query: String, athenaProperties: AthenaProperties): AthenaQueryResponse {
    val request = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext { it.database(athenaProperties.databaseName) }
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

  @Scheduled(cron = "0 0 0 1 * *")
  fun triggerRepairPartitions() = triggerRepairPartitions(AuditEventType.STAFF)

  @Scheduled(cron = "0 0 0 1 * *")
  fun triggerPrisonerRepairPartitions() = triggerRepairPartitions(AuditEventType.PRISONER)
}
