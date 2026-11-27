package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toPrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toStaffAuditEvent
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
) {

  data class ConsolidationTestEvents(
    val runId: String,
    val date: LocalDate,
    val events: List<AuditEvent>,
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

    val savedEvents = saveTestEvents(
      auditEventType,
      times.mapIndexed { index, time -> createTestEvent(time, runId, index) },
    ).sortedBy { it.`when` }

    log.info("Created {} {} consolidation test events for {}", savedEvents.size, auditEventType, targetTestDate)
    return ConsolidationTestEvents(runId, targetTestDate, savedEvents)
  }

  private fun checkSupported(auditEventType: AuditEventType) {
    if (auditEventType !in SUPPORTED_TYPES) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Consolidation tests are not supported for $auditEventType")
    }
  }

  private fun saveTestEvents(auditEventType: AuditEventType, events: List<AuditEvent>): List<AuditEvent> = when (auditEventType) {
    AuditEventType.STAFF -> staffAuditRepository.saveAll(events.map { it.toStaffAuditEvent() }).map { it.toAuditEvent() }
    AuditEventType.PRISONER -> prisonerAuditRepository.saveAll(events.map { it.toPrisonerAuditEvent() }).map { it.toAuditEvent() }
    else -> throw IllegalArgumentException("Unsupported audit event type $auditEventType")
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
    const val NUMBER_OF_TEST_EVENTS = 10
    const val TEST_EVENT_WHAT = "CONSOLIDATION_INTEGRATION_TEST"
    const val TEST_EVENT_WHO = "CONSOLIDATION_TEST"
    const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    val TEST_DATE_RANGE_START: LocalDate = LocalDate.of(1900, 1, 1)
    val TEST_DATE_RANGE_END_EXCLUSIVE: LocalDate = LocalDate.of(2023, 1, 1)
    val TEST_DATE_RANGE_DAYS: Long = ChronoUnit.DAYS.between(TEST_DATE_RANGE_START, TEST_DATE_RANGE_END_EXCLUSIVE)
  }
}
