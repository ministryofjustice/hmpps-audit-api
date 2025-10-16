package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditAthenaClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditServiceFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/internal/integration-test")
class AuditIntegrationTestController(
  private val auditQueueService: AuditQueueService,
  private val auditAthenaClient: AuditAthenaClient,
  private val auditServiceFactory: AuditServiceFactory,
) {

  data class IntegrationTestResult(
    val passed: Boolean,
    val message: String,
    val actualResult: List<AuditDto>?,
    val expectedResult: AuditEvent,
  )

  @PostMapping("/audit-event/{auditEventType}")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun createAuditEvent(@PathVariable auditEventType: AuditEventType): AuditEvent {
    val createdAuditEvent = createTestAuditEvent()
    if (auditEventType == AuditEventType.STAFF) {
      auditQueueService.sendAuditEvent(createdAuditEvent)
    } else if (auditEventType == AuditEventType.PRISONER) {
      auditQueueService.sendPrisonerAuditEvent(createdAuditEvent)
    }
    Thread.sleep(10000) // Time needed for event to be processed by SQS
    return createdAuditEvent
  }

  @PostMapping("/query/{auditEventType}/{who}")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun triggerTestQuery(
    @PathVariable auditEventType: AuditEventType,
    @PathVariable who: String,
  ): AuditQueryResponse = auditServiceFactory.getAuditService(auditEventType).triggerQuery(
    AuditQueryRequest(
      auditEventType = auditEventType,
      startDate = LocalDate.now(),
      who = who,
    ),
    auditEventType,
  )

  @GetMapping("/query/{auditEventType}/{queryExecutionId}")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun getQueryResults(
    @PathVariable auditEventType: AuditEventType,
    @PathVariable queryExecutionId: UUID,
  ): AuditQueryResponse = auditServiceFactory.getAuditService(auditEventType).getQueryResults(queryExecutionId.toString())

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
          IntegrationTestResult(false, "Test failed. Athena results were null", null, expectedAuditEvent),
        )
      }

      val matchFound = results.any {
        it.what == expectedAuditEvent.what &&
          it.`when` == expectedAuditEvent.`when` &&
          it.operationId == expectedAuditEvent.operationId &&
          it.subjectId == expectedAuditEvent.subjectId &&
          it.subjectType == expectedAuditEvent.subjectType &&
          it.correlationId == expectedAuditEvent.correlationId &&
          it.who == expectedAuditEvent.who &&
          it.service == expectedAuditEvent.service &&
          it.details == expectedAuditEvent.details
      }

      if (matchFound) {
        ResponseEntity.ok(
          IntegrationTestResult(true, "Test successful. Audit event found in Athena", results, expectedAuditEvent),
        )
      } else {
        ResponseEntity.internalServerError().body(
          IntegrationTestResult(false, "Test failed. Audit event not found in Athena", results, expectedAuditEvent),
        )
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
      ResponseEntity.internalServerError().body(
        IntegrationTestResult(false, "Exception during test: ${ex.message}", null, expectedAuditEvent),
      )
    }
  }

  private fun createTestAuditEvent(): AuditEvent = AuditEvent(
    what = "INTEGRATION_TEST",
    `when` = Instant.now(),
    operationId = UUID.randomUUID().toString(),
    subjectId = "some subject ID",
    subjectType = "some subject type",
    correlationId = UUID.randomUUID().toString(),
    who = "TEST_" + (1..5).map { ('A'..'Z').random() }.joinToString(""), // Random who to create a unique partition on every run
    service = "some service",
    details = "{\"key\": \"value\"}",
  )
}
