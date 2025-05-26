package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AthenaPartitionRepairService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.time.Duration.Companion.seconds

@RestController
@RequestMapping("/internal/test")
class AuditFlowTestController(
  private val auditQueueService: AuditQueueService,
  private val athenaPartitionRepairService: AthenaPartitionRepairService,
  private val auditService: AuditService,
) {

  data class IntegrationTestResult(
    val passed: Boolean,
    val message: String,
    val actualResult: DigitalServicesQueryResponse?,
  )

  @PostMapping("/audit-end-to-end-test")
  fun runAuditIntegrationTest(): ResponseEntity<IntegrationTestResult> = runBlocking {
    val testEvent = createTestAuditEvent()

    // Step 1: Send event to SQS
    auditQueueService.sendAuditEvent(testEvent)

    // Step 2: Wait for ingestion + Athena readiness
    delay(5.seconds) // crude but simple for now

    // Step 3: Update partitions
    athenaPartitionRepairService.repairPartitions()

    // Step 4: Trigger query
    val queryRequest = DigitalServicesQueryRequest(
      who = testEvent.who,
      startDate = LocalDate.now(),
    )

    val queryResponse = auditService.triggerQuery(queryRequest)
    val queryId = queryResponse.queryExecutionId.toString()

    // Step 5: Poll Athena until query succeeds or times out
    val result = pollUntilSucceeded(queryId)

    // Step 6: Verify result
    val matchFound = result.results?.any {
      it.id == testEvent.id &&
        it.who == testEvent.who &&
        it.what == testEvent.what
    } ?: false

    if (matchFound) {
      return@runBlocking ResponseEntity.ok(
        IntegrationTestResult(true, "Audit event found in Athena", result),
      )
    }

    ResponseEntity.internalServerError().body(
      IntegrationTestResult(false, "Audit event not found in Athena", result),
    )
  }

  private fun createTestAuditEvent(): HMPPSAuditListener.AuditEvent = HMPPSAuditListener.AuditEvent(
    what = "INTEGRATION_TEST",
    `when` = Instant.now(),
    operationId = UUID.randomUUID().toString(),
    subjectId = "some subject ID",
    subjectType = "some subjectType ID",
    correlationId = UUID.randomUUID().toString(),
    who = "some user",
    service = "some service",
    details = "{\"key\": \"value\"}",
  )

  private suspend fun pollUntilSucceeded(queryExecutionId: String): DigitalServicesQueryResponse {
    var result: DigitalServicesQueryResponse
    repeat(12) {
      // retry for up to ~1 minute
      result = auditService.getQueryResults(queryExecutionId)
      if (result.queryState.toString() == "SUCCEEDED") {
        return result
      }
      delay(2.seconds)
    }
    throw IllegalStateException("Athena query $queryExecutionId did not complete in time")
  }
}
