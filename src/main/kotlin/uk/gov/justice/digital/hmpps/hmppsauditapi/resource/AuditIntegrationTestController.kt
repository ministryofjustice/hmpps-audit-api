package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
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
    return try {
      val results = auditAthenaClient.getAuditEventsQueryResults(queryExecutionId).results

      if (results == null) {
        return ResponseEntity.internalServerError().body(
          IntegrationTestResult(false, "Test failed. Athena results were null", null),
        )
      }

      val matchFound = results.any {
        it.`when` == expectedAuditEvent.`when` &&
          it.who == expectedAuditEvent.who &&
          it.what == expectedAuditEvent.what &&
          it.details == expectedAuditEvent.details
      }

      if (matchFound) {
        ResponseEntity.ok(
          IntegrationTestResult(true, "Test successful. Audit event found in Athena", results),
        )
      } else {
        ResponseEntity.internalServerError().body(
          IntegrationTestResult(false, "Test failed. Audit event not found in Athena", results),
        )
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
      ResponseEntity.internalServerError().body(
        IntegrationTestResult(false, "Exception during test: ${ex.message}", null),
      )
    }
  }

  private fun createTestAuditEvent(): AuditEvent = AuditEvent(
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

}
