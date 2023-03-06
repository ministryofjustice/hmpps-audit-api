package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
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
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class AuditService(

  private val telemetryClient: TelemetryClient,
  private val auditRepository: AuditRepository,
  private val mapper: ObjectMapper,
  private val hmppsQueueService: HmppsQueueService,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun audit(auditEvent: AuditEvent) {
    telemetryClient.trackEvent("hmpps-audit", auditEvent.asMap())
    auditRepository.save(auditEvent)
  }

  fun findAll(): List<AuditDto> = auditRepository.findAll(Sort.by(DESC, "when")).map { AuditDto(it) }

  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto> {
    with(auditFilterDto) {
      log.info(
        "Searching audit events by startDate {} endDate {} service {} what {} who {}",
        startDateTime,
        endDateTime,
        service,
        what,
        who,
      )
      return auditRepository.findPage(
        pageable,
        startDateTime,
        endDateTime,
        service,
        what,
        who,
      )
        .map { AuditDto(it) }
    }
  }

  fun sendAuditEvent(auditEvent: AuditEvent) {
    val hmppsQueue =
      hmppsQueueService.findByQueueId("auditqueue") ?: throw IllegalStateException("Unable to find auditqueue")
    with(hmppsQueue) {
      sqsClient.sendMessage(queueUrl, mapper.writeValueAsString(auditEvent))
    }
  }
}

private fun AuditEvent.asMap(): Map<String, String> {
  val items = mutableMapOf("what" to what, "when" to `when`.toString())
  items.addIfNotNull("who", who)
  items.addIfNotNull("operationId", operationId)
  items.addIfNotNull("service", service)
  return items.toMap()
}

fun MutableMap<String, String>.addIfNotNull(key: String, value: String?) {
  value?.let { this.put(key, value) }
}
