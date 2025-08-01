package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
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

@RestController
@RequestMapping("/internal/integration-test")
class AuditIntegrationTestController(
  private val auditQueueService: AuditQueueService,
  private val auditAthenaClient: AuditAthenaClient,
  private val auditService: AuditService,
  private val telemetryClient: TelemetryClient,
) {

  data class IntegrationTestResult(
    val passed: Boolean,
    val message: String,
    val actualResult: List<AuditDto>?,
  )

  @PostMapping("/audit-event")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun createAuditEvent(): AuditDto {
    val createdAuditEvent = createTestAuditEvent()
    auditQueueService.sendAuditEvent(createdAuditEvent.toEntity())
    Thread.sleep(10000) // Time needed for event to be processed by SQS
    return createdAuditEvent
  }

  @PostMapping("/query/{who}")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun queryTestAuditEvent(@PathVariable who: String): AthenaQueryResponse = auditService.triggerQuery(
    DigitalServicesQueryRequest(
      startDate = LocalDate.now(),
      who = who,
    ),
    AuditEventType.STAFF,
  )

  @PostMapping("/assertion/{queryExecutionId}")
  @PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
  fun assertAuditEventSavedCorrectly(
    @RequestBody expectedAuditEvent: AuditDto,
    @PathVariable queryExecutionId: String,
  ): ResponseEntity<IntegrationTestResult> {
    telemetryClient.trackEvent("mohamad", mapOf("event" to expectedAuditEvent.toString()))

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

      telemetryClient.trackEvent("mohamad", mapOf("results" to results.toString()))

      if (matchFound) {
        ResponseEntity.ok(
          IntegrationTestResult(true, "Test successful. Audit event found in Athena", results),
        )
      } else {
        ResponseEntity.internalServerError().body(
          IntegrationTestResult(false, "Test failed. Expected $expectedAuditEvent but got $results", results),
        )
      }
    } catch (ex: Exception) {
      ex.printStackTrace()
      ResponseEntity.internalServerError().body(
        IntegrationTestResult(false, "Exception during test: ${ex.message}", null),
      )
    }
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  fun handleDeserializationError(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> {
    ex.printStackTrace()
    return ResponseEntity.badRequest().body(
      mapOf(
        "error" to "Malformed JSON or incorrect field types",
        "message" to (ex.mostSpecificCause?.message ?: ex.message ?: "Unknown error"),
      ),
    )
  }

  @ExceptionHandler(Exception::class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  fun handleOtherExceptions(ex: Exception): ResponseEntity<Map<String, String>> {
    ex.printStackTrace()
    return ResponseEntity.internalServerError().body(
      mapOf(
        "error" to "Unexpected server error",
        "message" to (ex.message ?: "Unknown error"),
      ),
    )
  }

  private fun createTestAuditEvent(): AuditDto = AuditDto(
    id = UUID.randomUUID(),
    what = "INTEGRATION_TEST",
    `when` = Instant.now(),
    operationId = UUID.randomUUID().toString(),
    subjectId = "some subject ID",
    subjectType = "some subjectType ID",
    correlationId = UUID.randomUUID().toString(),
    who = "TEST_" + (1..5).map { ('A'..'Z').random() }.joinToString(""), // Random who to create a unique partition on every run
    service = "some service",
    details = "{\"key\": \"value\"}",
  )

  private fun AuditDto.toEntity() = AuditEvent(
    id = id,
    what = what,
    `when` = `when`,
    operationId = operationId,
    subjectId = subjectId,
    subjectType = subjectType ?: "UNKNOWN",
    correlationId = correlationId,
    who = who,
    service = service,
    details = details,
  )
}
