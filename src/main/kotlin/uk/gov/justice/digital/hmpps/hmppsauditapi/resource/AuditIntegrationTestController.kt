package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AthenaPartitionRepairService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditAthenaClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private const val WHO = "audit-integration-test-user"

@RestController
@RequestMapping("/internal/integration-test")
class AuditIntegrationTestController(
  private val auditQueueService: AuditQueueService,
  private val athenaPartitionRepairService: AthenaPartitionRepairService,
  private val auditAthenaClient: AuditAthenaClient,
  private val auditService: AuditService,
) {

  data class IntegrationTestResult(
    val passed: Boolean,
    val message: String,
    val actualResult: List<AuditDto>?,
  )

  @PostMapping("/audit-event")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun createAuditEvent(): AuditEvent {
    val createdAuditEvent = createTestAuditEvent()
    auditQueueService.sendAuditEvent(createdAuditEvent)
    Thread.sleep(10000) // Time needed for event to be processed by SQS
    return createdAuditEvent
  }

  @PostMapping("/query")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun queryTestAuditEvent(): AthenaQueryResponse = auditService.triggerQuery(
    DigitalServicesQueryRequest(
      startDate = LocalDate.now(),
      who = WHO,
    ),
    AuditEventType.STAFF,
  )

  @PostMapping("/assertion/{queryExecutionId}")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun assertAuditEventSavedCorrectly(
    @RequestBody expectedAuditEvent: AuditEvent,
    @PathVariable queryExecutionId: String,
  ): ResponseEntity<IntegrationTestResult> {
    val results = auditAthenaClient.getQueryResults(queryExecutionId).results

    val matchFound = results?.any {
//      it.`when` == expectedAuditEvent.`when` &&
        it.who == expectedAuditEvent.who &&
                it.what == expectedAuditEvent.what &&
                it.details == expectedAuditEvent.details
    } == true

    if (matchFound) {
      return ResponseEntity.ok(
        IntegrationTestResult(true, "Test successful. Audit event found in Athena", results),
      )
    }

    return ResponseEntity.internalServerError().body(
      IntegrationTestResult(false, "Test failed. Audit event not found in Athena", results),
    )
  }

//  @PostMapping("/audit-end-to-end-test")
//  fun runAuditIntegrationTest(): ResponseEntity<IntegrationTestResult> {
//    val testEvent = createTestAuditEvent()
//
//    // Step 1: Send event to SQS
//    auditQueueService.sendAuditEvent(testEvent)
//
//    // Step 2: Wait for ingestion + Athena readiness
//    Thread.sleep(10000)
//
//    // Step 3: Update partitions
//    athenaPartitionRepairService.triggerRepairPartitions(AuditEventType.STAFF)
//    Thread.sleep(10000)
//
//    // Step 4: Trigger query
//    val queryRequest = DigitalServicesQueryRequest(
//      who = testEvent.who,
//      startDate = LocalDate.now(),
//    )
//    val queryResponse = auditService.triggerQuery(queryRequest, AuditEventType.STAFF)
//
//    // Step 5: Poll Athena until query succeeds or times out
//    val queryId = queryResponse.queryExecutionId.toString()
//    val result = pollUntilSucceeded(queryId)
//
//    // Step 6: Verify result
//    val matchFound = result.results?.any {
//      it.`when` == testEvent.`when` &&
//        it.who == testEvent.who &&
//        it.what == testEvent.what &&
//        it.details == testEvent.details
//    } ?: false
//
//    if (matchFound) {
//      return ResponseEntity.ok(
//        IntegrationTestResult(true, "Test successful. Audit event found in Athena", result),
//      )
//    }
//
//    return ResponseEntity.internalServerError().body(
//      IntegrationTestResult(false, "Test failed. Audit event not found in Athena", result),
//    )
//  }

  private fun createTestAuditEvent(): AuditEvent = HMPPSAuditListener.AuditEvent(
    what = "INTEGRATION_TEST",
    `when` = Instant.now(),
    operationId = UUID.randomUUID().toString(),
    subjectId = "some subject ID",
    subjectType = "some subjectType ID",
    correlationId = UUID.randomUUID().toString(),
    who = WHO,
    service = "some service",
    details = "{\"key\": \"value\"}",
  )

  private fun pollUntilSucceeded(queryExecutionId: String): AthenaQueryResponse {
    var result: AthenaQueryResponse
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
