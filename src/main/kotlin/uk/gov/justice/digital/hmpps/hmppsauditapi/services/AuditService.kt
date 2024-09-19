package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.util.UUID

@Service
class AuditService(
  private val telemetryClient: TelemetryClient,
  private val auditRepository: AuditRepository,
  private val auditS3Client: AuditS3Client,
  @Value("\${hmpps.repository.saveToS3Bucket}") private val saveToS3Bucket: Boolean,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun audit(auditEvent: AuditEvent) {
    telemetryClient.trackEvent("hmpps-audit", auditEvent.asMap())
    if (saveToS3Bucket) {
      auditEvent.id = UUID.randomUUID()
      auditS3Client.save(auditEvent)
    } else {
      auditRepository.save(auditEvent)
    }
  }

  fun findAll(): List<AuditDto> = auditRepository.findAll(Sort.by(DESC, "when")).map { AuditDto(it) }

  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto> {
    with(auditFilterDto) {
      log.info(
        "Searching audit events by startDate {} endDate {} service {} subjectId {} subjectType {} correlationId {} what {} who {}",
        startDateTime,
        endDateTime,
        service,
        subjectId,
        subjectType,
        correlationId,
        what,
        who,
      )
      return auditRepository.findPage(
        pageable,
        startDateTime,
        endDateTime,
        service,
        subjectId,
        subjectType,
        correlationId,
        what,
        who,
      )
        .map { AuditDto(it) }
    }
  }
}

private fun AuditEvent.asMap(): Map<String, String> {
  val items = mutableMapOf("what" to what, "when" to `when`.toString())
  items.addIfNotNull("who", who)
  items.addIfNotNull("operationId", operationId)
  items.addIfNotNull("subjectId", subjectId)
  items.addIfNotNull("subjectType", subjectType)
  items.addIfNotNull("correlationId", correlationId)
  items.addIfNotNull("service", service)
  return items.toMap()
}
