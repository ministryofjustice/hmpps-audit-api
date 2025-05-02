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
  @Value("\${aws.athena.workgroup}") private val workGroup: String,
  @Value("\${aws.athena.outputLocation}") private val outputLocation: String,
) {

  @Scheduled(cron = "0 /5 * * * *")
  fun repairPartitions() {
    val repairQuery = "MSCK REPAIR TABLE $databaseName.audit_event;"
    val request = StartQueryExecutionRequest.builder()
      .queryString(repairQuery)
      .queryExecutionContext { it.database(databaseName) }
      .workGroup(workGroup)
      .resultConfiguration { it.outputLocation(outputLocation) }
      .build()
    athenaClient.startQueryExecution(request)
  }
}
