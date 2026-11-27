package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditConsolidationService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

@RestController
@Profile("aws-testing")
@RequestMapping("/internal/dev-testing")
@PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
class DevTestingController(
  private val staffAuditRepository: StaffAuditRepository,
  private val prisonerAuditRepository: PrisonerAuditRepository,
  private val auditQueueService: AuditQueueService,
  private val auditConsolidationService: AuditConsolidationService,
) {

  data class ConsolidationTestEvents(
    val runId: String,
    val date: LocalDate,
    val events: List<AuditEvent>,
  )

  data class ConsolidationTestRun(
    val runId: String,
    val date: LocalDate,
  )

  @PostMapping("/consolidation/{auditEventType}/test-events")
  fun createConsolidationTestEvents(@PathVariable auditEventType: AuditEventType): ConsolidationTestEvents {
    checkSupported(auditEventType)
    val targetTestDate = TEST_DATE_RANGE_START.plusDays(Random.nextLong(TEST_DATE_RANGE_DAYS))
    val startOfTargetTestDate = targetTestDate.atStartOfDay().toInstant(ZoneOffset.UTC)

    // Distinct whole-millisecond times so the consolidated file order (when, id) is deterministic
    val times = generateSequence { Random.nextLong(MILLIS_PER_DAY) }.distinct().take(NUMBER_OF_TEST_EVENTS).sorted()
      .map { startOfTargetTestDate.plusMillis(it) }
      .toList()
    val runId = UUID.randomUUID().toString()

    times.mapIndexed { index, time -> createTestEvent(time, runId, index) }.forEach { sendTestEvent(auditEventType, it) }
    val savedEvents = waitForTestEventsToBeSaved(auditEventType, runId, targetTestDate)

    log.info("Created {} {} consolidation test events for {}", savedEvents.size, auditEventType, targetTestDate)
    return ConsolidationTestEvents(runId, targetTestDate, savedEvents)
  }

  @PostMapping("/consolidation/{auditEventType}/consolidations")
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun startConsolidation(
    @PathVariable auditEventType: AuditEventType,
    @RequestBody testRun: ConsolidationTestRun,
  ) {
    checkSupported(auditEventType)
    findRequiredTestRunEvents(auditEventType, testRun)
    auditConsolidationService.startConsolidationJob(auditEventType, testRun.date)
    log.info("Started {} consolidation for test run {} on {}", auditEventType, testRun.runId, testRun.date)
  }

  private fun findRequiredTestRunEvents(auditEventType: AuditEventType, testRun: ConsolidationTestRun): List<AuditEvent> = findTestRunEvents(auditEventType, testRun.runId, testRun.date).ifEmpty {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No events found for consolidation test run ${testRun.runId} on ${testRun.date}")
  }

  private fun checkSupported(auditEventType: AuditEventType) {
    if (auditEventType !in SUPPORTED_TYPES) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Consolidation tests are not supported for $auditEventType")
    }
  }

  private fun sendTestEvent(auditEventType: AuditEventType, event: AuditEvent) = when (auditEventType) {
    AuditEventType.STAFF -> auditQueueService.sendAuditEvent(event)
    AuditEventType.PRISONER -> auditQueueService.sendPrisonerAuditEvent(event)
    else -> throw IllegalArgumentException("Unsupported audit event type $auditEventType")
  }

  // Events are saved asynchronously by the queue listener, so wait for all of them before consolidation is triggered
  private fun waitForTestEventsToBeSaved(auditEventType: AuditEventType, runId: String, date: LocalDate): List<AuditEvent> {
    val deadline = Instant.now().plus(SAVE_TIMEOUT)
    var savedEvents = findTestRunEvents(auditEventType, runId, date)
    while (savedEvents.size < NUMBER_OF_TEST_EVENTS && Instant.now().isBefore(deadline)) {
      Thread.sleep(SAVE_POLL_INTERVAL)
      savedEvents = findTestRunEvents(auditEventType, runId, date)
    }
    if (savedEvents.size < NUMBER_OF_TEST_EVENTS) {
      throw ResponseStatusException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Only ${savedEvents.size} of $NUMBER_OF_TEST_EVENTS $auditEventType test events for run $runId were saved within $SAVE_TIMEOUT",
      )
    }
    return savedEvents
  }

  private fun findTestRunEvents(auditEventType: AuditEventType, runId: String, date: LocalDate): List<AuditEvent> {
    val start = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    val end = start.plusMillis(MILLIS_PER_DAY)
    return when (auditEventType) {
      AuditEventType.STAFF ->
        staffAuditRepository
          .findAllByWhatAndCorrelationIdAndWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(TEST_EVENT_WHAT, runId, start, end)
          .map { it.toAuditEvent() }
      AuditEventType.PRISONER ->
        prisonerAuditRepository
          .findAllByWhatAndCorrelationIdAndWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(TEST_EVENT_WHAT, runId, start, end)
          .map { it.toAuditEvent() }
      else -> throw IllegalArgumentException("Unsupported audit event type $auditEventType")
    }
  }

  private fun createTestEvent(time: Instant, runId: String, index: Int) = AuditEvent(
    what = TEST_EVENT_WHAT,
    `when` = time,
    operationId = UUID.randomUUID().toString(),
    subjectId = "subject-$index",
    subjectType = "CONSOLIDATION_TEST",
    correlationId = runId,
    who = TEST_EVENT_WHO,
    service = "hmpps-audit-api",
    details = "{\"index\": $index}",
  )

  private companion object {
    val log: Logger = LoggerFactory.getLogger(DevTestingController::class.java)
    val SUPPORTED_TYPES = setOf(AuditEventType.STAFF, AuditEventType.PRISONER)
    const val NUMBER_OF_TEST_EVENTS = 5
    val SAVE_TIMEOUT: Duration = Duration.ofSeconds(15)
    val SAVE_POLL_INTERVAL: Duration = Duration.ofMillis(500)
    const val TEST_EVENT_WHAT = "CONSOLIDATION_INTEGRATION_TEST"
    const val TEST_EVENT_WHO = "CONSOLIDATION_TEST"
    const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    val TEST_DATE_RANGE_START: LocalDate = LocalDate.of(1900, 1, 1)
    val TEST_DATE_RANGE_END_EXCLUSIVE: LocalDate = LocalDate.of(2023, 1, 1)
    val TEST_DATE_RANGE_DAYS: Long = ChronoUnit.DAYS.between(TEST_DATE_RANGE_START, TEST_DATE_RANGE_END_EXCLUSIVE)
  }
}
