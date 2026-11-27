package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AuditConsolidationConfig
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import java.nio.file.Files
import java.time.LocalDate

@Service
class AuditConsolidationService(
  private val config: AuditConsolidationConfig,
  private val consolidationWriter: AuditConsolidationWriter,
  private val auditS3Client: AuditS3Client,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
  @Qualifier("auditConsolidationExecutor") private val taskExecutor: TaskExecutor,
) {
  fun startConsolidationJob(eventType: AuditEventType, queryDate: LocalDate) {
    checkFeatureFlagEnabled(eventType)
    try {
      taskExecutor.execute {
        try {
          consolidateAuditEventsOnDate(eventType, queryDate)
        } catch (e: Exception) {
          log.error("{} audit consolidation for {} failed; resubmit to retry", eventType, queryDate, e)
        }
      }
    } catch (e: TaskRejectedException) {
      log.warn("{} audit consolidation queue is full or shutting down for {}", eventType, queryDate, e)
      throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audit consolidation queue is unavailable", e)
    }
  }

  private fun consolidateAuditEventsOnDate(eventType: AuditEventType, queryDate: LocalDate) {
    checkFeatureFlagEnabled(eventType)
    log.info("Starting {} audit consolidation for {}", eventType, queryDate)
    val file = Files.createTempFile("${eventType.name.lowercase()}-audit-consolidation-$queryDate-", ".parquet")
    try {
      val numberOfEvents = consolidationWriter.write(eventType, queryDate, file)
      val s3BucketName = athenaPropertiesFactory.getProperties(eventType).s3BucketName
      auditS3Client.saveConsolidatedAuditEvents(file, s3BucketName, generateFilename(eventType, queryDate))
      log.info("Completed {} audit consolidation for {} with {} events", eventType, queryDate, numberOfEvents)
    } finally {
      Files.deleteIfExists(file)
    }
  }

  private fun checkFeatureFlagEnabled(type: AuditEventType) {
    if (!config.featureFlagEnabled) {
      log.warn("{} audit consolidation requested while consolidations are disabled", type)
      throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Audit consolidations are disabled")
    }
    if (type !in config.enabledAuditEventTypes) {
      log.info("Audit consolidations are not enabled for {}", type)
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Audit consolidations are not enabled for $type")
    }
  }

  private fun generateFilename(eventType: AuditEventType, date: LocalDate) = "${eventType.name.lowercase()}/year=${date.year}/month=${date.monthValue}/day=${date.dayOfMonth}/audit-events.parquet"

  private companion object {
    val log: Logger = LoggerFactory.getLogger(AuditConsolidationService::class.java)
  }
}
