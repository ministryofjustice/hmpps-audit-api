package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent

@Component
class AthenaPartitionRepairService(
  private val athenaClient: AthenaClient,
  private val telemetryClient: TelemetryClient,
  @Value("\${aws.athena.database}") private val databaseName: String,
  @Value("\${aws.athena.workgroup}") private val workGroup: String,
  @Value("\${aws.athena.outputLocation}") private val outputLocation: String,
) {

  @Scheduled(cron = "0 */10 * * * *")
  fun repairPartitions() {
    try {
      val repairQuery = "MSCK REPAIR TABLE $databaseName.audit_event;"

      val request = StartQueryExecutionRequest.builder()
        .queryString(repairQuery)
        .queryExecutionContext { it.database(databaseName) }
        .workGroup(workGroup)
        .resultConfiguration { it.outputLocation(outputLocation) }
        .build()

      val response = athenaClient.startQueryExecution(request)
      telemetryClient.trackEvent("partition", mapOf("partition query execution ID" to response.queryExecutionId()))
    } catch (ex: Exception) {
      telemetryClient.trackEvent("partition-error", mapOf("error" to ex.message.toString()))
    }
  }
}
