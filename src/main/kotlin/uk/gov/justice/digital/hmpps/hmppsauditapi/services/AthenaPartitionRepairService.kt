package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest

@Component
class AthenaPartitionRepairService(
  private val athenaClient: AthenaClient,
  @Value("\${aws.athena.database}") private val databaseName: String,
  @Value("\${aws.athena.table}") private val tableName: String,
  @Value("\${aws.athena.workgroup}") private val workGroupName: String,
  @Value("\${aws.athena.outputLocation}") private val outputLocation: String,
  @Value("\${aws.athena.prisonerDatabase}") private val prisonerDatabaseName: String,
  @Value("\${aws.athena.prisonerTable}") private val prisonerTableName: String,
  @Value("\${aws.athena.prisonerWorkgroup}") private val prisonerWorkGroupName: String,
  @Value("\${aws.athena.prisonerOutputLocation}") private val prisonerOutputLocation: String,
) {

  @Scheduled(cron = "0 0 * * * *")
  fun repairPartitions() {
    val repairQuery = "MSCK REPAIR TABLE $databaseName.$tableName;"
    val request = StartQueryExecutionRequest.builder()
      .queryString(repairQuery)
      .queryExecutionContext { it.database(databaseName) }
      .workGroup(workGroupName)
      .resultConfiguration { it.outputLocation(outputLocation) }
      .build()
    athenaClient.startQueryExecution(request)
  }

  @Scheduled(cron = "0 0 * * * *")
  fun repairPrisonerPartitions() {
    val repairQuery = "MSCK REPAIR TABLE $prisonerDatabaseName.$prisonerTableName;"
    val request = StartQueryExecutionRequest.builder()
      .queryString(repairQuery)
      .queryExecutionContext { it.database(prisonerDatabaseName) }
      .workGroup(prisonerWorkGroupName)
      .resultConfiguration { it.outputLocation(prisonerOutputLocation) }
      .build()
    athenaClient.startQueryExecution(request)
  }
}
