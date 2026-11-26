package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.io.LocalInputFile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toPrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toStaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditS3Client
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.generateFilename
import java.nio.file.Files
import java.nio.file.Path
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
  private val auditS3Client: AuditS3Client,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
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

  data class ConsolidationTestResult(
    val passed: Boolean,
    val message: String,
    val actualResult: List<AuditEvent>?,
    val expectedResult: List<AuditEvent>,
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

  @PostMapping("/consolidation/{auditEventType}/assertion")
  fun assertConsolidatedFile(
    @PathVariable auditEventType: AuditEventType,
    @RequestBody testRun: ConsolidationTestRun,
  ): ResponseEntity<ConsolidationTestResult> {
    checkSupported(auditEventType)
    val testRunEvents = findRequiredTestRunEvents(auditEventType, testRun)
    val testRunEventIds = testRunEvents.mapNotNull { it.id }.toSet()
    val bucketName = athenaPropertiesFactory.getProperties(auditEventType).s3BucketName
    val key = generateFilename(auditEventType, testRun.date)
    val file = Files.createTempFile("${auditEventType.name.lowercase()}-consolidation-test-${testRun.date}-", ".parquet")

    try {
      if (!auditS3Client.download(bucketName, key, file)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          ConsolidationTestResult(false, "Consolidated file s3://$bucketName/$key not found yet", null, testRunEvents),
        )
      }
      val fileContents = tryReadAuditEvents(file, testRunEventIds, bucketName, key)
      cleanUp(auditEventType, testRun, testRunEventIds, bucketName, key, fileContents.getOrNull()?.containsOnly(testRunEventIds) == true)
      return fileContents.fold(
        onSuccess = { assertionResult(it.matchingEvents, testRunEvents) },
        onFailure = { ResponseEntity.internalServerError().body(ConsolidationTestResult(false, "Exception during test: ${it.message}", null, testRunEvents)) },
      )
    } finally {
      Files.deleteIfExists(file)
    }
  }

  // Only events persisted by this test run are asserted on or deleted, so other audit data is never touched
  private fun findRequiredTestRunEvents(auditEventType: AuditEventType, testRun: ConsolidationTestRun): List<AuditEvent> = findTestRunEvents(auditEventType, testRun.runId, testRun.date).ifEmpty {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No events found for consolidation test run ${testRun.runId} on ${testRun.date}")
  }

  private fun tryReadAuditEvents(file: Path, ids: Set<UUID>, bucketName: String, key: String): Result<ConsolidatedFileContents> = try {
    Result.success(readAuditEvents(file, ids))
  } catch (ex: Exception) {
    log.error("Failed to read consolidated file s3://{}/{}", bucketName, key, ex)
    Result.failure(ex)
  }

  private fun assertionResult(actualEvents: List<AuditEvent>, expectedEvents: List<AuditEvent>): ResponseEntity<ConsolidationTestResult> = if (actualEvents == expectedEvents) {
    ResponseEntity.ok(ConsolidationTestResult(true, "Test successful. Consolidated file contains the test events", actualEvents, expectedEvents))
  } else {
    ResponseEntity.internalServerError().body(
      ConsolidationTestResult(false, "Test failed. Consolidated file does not contain the expected test events", actualEvents, expectedEvents),
    )
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

  private fun deleteTestEvents(auditEventType: AuditEventType, ids: Collection<UUID>) = when (auditEventType) {
    AuditEventType.STAFF -> staffAuditRepository.deleteAllById(ids)
    AuditEventType.PRISONER -> prisonerAuditRepository.deleteAllById(ids)
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

  private data class ConsolidatedFileContents(val matchingEvents: List<AuditEvent>, val totalEvents: Long) {
    // The file may also contain other events for the same day; it only belongs to the test run if it holds nothing else
    fun containsOnly(ids: Set<UUID>) = totalEvents == ids.size.toLong() &&
      matchingEvents.size == ids.size &&
      matchingEvents.mapNotNull { it.id }.toSet() == ids
  }

  private fun readAuditEvents(file: Path, ids: Set<UUID>): ConsolidatedFileContents = AvroParquetReader.builder<GenericRecord>(LocalInputFile(file)).build().use { reader ->
    var totalEvents = 0L
    val matchingEvents = generateSequence { reader.read() }
      .onEach { totalEvents++ }
      .filter { UUID.fromString(it.get("id").toString()) in ids }
      .map { it.toAuditEvent() }
      .toList()
    ConsolidatedFileContents(matchingEvents, totalEvents)
  }

  private fun GenericRecord.toAuditEvent() = AuditEvent(
    id = UUID.fromString(get("id").toString()),
    what = get("what").toString(),
    `when` = Instant.parse(get("when").toString()),
    operationId = get("operationId")?.toString(),
    subjectId = get("subjectId")?.toString(),
    subjectType = get("subjectType").toString(),
    correlationId = get("correlationId")?.toString(),
    who = get("who")?.toString(),
    service = get("service")?.toString(),
    details = get("details")?.toString(),
  )

  private fun cleanUp(
    auditEventType: AuditEventType,
    testRun: ConsolidationTestRun,
    testRunEventIds: Set<UUID>,
    bucketName: String,
    key: String,
    deleteFile: Boolean,
  ) {
    try {
      deleteTestEvents(auditEventType, testRunEventIds)
      if (deleteFile) {
        auditS3Client.delete(bucketName, key)
      } else {
        log.warn("Leaving s3://{}/{} in place as it does not contain only test run {} events", bucketName, key, testRun.runId)
      }
      log.info("Cleaned up {} consolidation test run {} for {}", auditEventType, testRun.runId, testRun.date)
    } catch (ex: Exception) {
      log.error("Failed to clean up {} consolidation test run {} for {}", auditEventType, testRun.runId, testRun.date, ex)
    }
  }

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
