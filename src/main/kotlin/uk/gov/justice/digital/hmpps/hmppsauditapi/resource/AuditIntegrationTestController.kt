package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

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
import java.util.UUID

// TODO only expose endpoint in dev
@RestController
@RequestMapping("/internal/test")
class AuditIntegrationTestController(
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
  fun runAuditIntegrationTest(): ResponseEntity<IntegrationTestResult> {
    val testEvent = createTestAuditEvent()

    // Step 1: Send event to SQS
    auditQueueService.sendAuditEvent(testEvent)

    // Step 2: Wait for ingestion + Athena readiness
    Thread.sleep(10000)

    // Step 3: Update partitions
    athenaPartitionRepairService.repairPartitions()
    Thread.sleep(2000)

    // Step 4: Trigger query
    val queryRequest = DigitalServicesQueryRequest(
      who = testEvent.who,
      startDate = LocalDate.now(),
    )
    val queryResponse = auditService.triggerQuery(queryRequest)

    // Step 5: Poll Athena until query succeeds or times out
    val queryId = queryResponse.queryExecutionId.toString()
    val result = pollUntilSucceeded(queryId)

    // Step 6: Verify result
    val matchFound = result.results?.any {
      it.`when` == testEvent.`when` &&
        it.who == testEvent.who &&
        it.what == testEvent.what &&
        it.operationId == testEvent.operationId &&
        it.subjectId == testEvent.subjectId &&
        it.subjectType == testEvent.subjectType &&
        it.correlationId == testEvent.correlationId &&
        it.service == testEvent.service &&
        it.details == testEvent.details
    } ?: false

    if (matchFound) {
      return ResponseEntity.ok(
        IntegrationTestResult(true, "Test successful. Audit event found in Athena", result),
      )
    }

    return ResponseEntity.internalServerError().body(
      IntegrationTestResult(false, "Test failed. Audit event not found in Athena", result),
    )
  }

  private fun createTestAuditEvent(): HMPPSAuditListener.AuditEvent = HMPPSAuditListener.AuditEvent(
    what = "INTEGRATION_TEST",
    `when` = Instant.now(),
    operationId = UUID.randomUUID().toString(),
    subjectId = "some subject ID",
    subjectType = "some subjectType ID",
    correlationId = UUID.randomUUID().toString(),
    who = "INTEGRATION_TEST_USER",
    service = "some service",
    details = "{\"key\": \"value\"}",
  )

  private fun pollUntilSucceeded(queryExecutionId: String): DigitalServicesQueryResponse {
    var result: DigitalServicesQueryResponse
    repeat(12) {
      // retry for up to ~1 minute
      result = auditService.getQueryResults(queryExecutionId)
      if (result.queryState.toString() == "SUCCEEDED") {
        return result
      }
      Thread.sleep(1000)
    }
    throw IllegalStateException("Athena query $queryExecutionId did not complete in time")
  }
}
