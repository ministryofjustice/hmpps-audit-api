package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.SqsConfigProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.mainQueue
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto

@Service
class AuditService(
  private val sqsConfigProperties: SqsConfigProperties,
  private val telemetryClient: TelemetryClient,
  private val auditRepository: AuditRepository,
  private val mapper: ObjectMapper,
  private val auditMessagingTemplate: QueueMessagingTemplate
) {

  fun audit(auditEvent: AuditEvent) {
    telemetryClient.trackEvent("hmpps-audit", auditEvent.asMap())
    auditRepository.save(auditEvent)
  }

  fun findAll(): List<AuditDto> = auditRepository.findAll(Sort.by(DESC, "when")).map { AuditDto(it) }

  fun findPage(pageable: Pageable = Pageable.unpaged(), who: String?, what: String?): Page<AuditDto> =
    auditRepository.findPage(pageable, who, what).map { AuditDto(it) }

  fun sendAuditEvent(auditEvent: AuditEvent) {
    auditMessagingTemplate.send(
      sqsConfigProperties.mainQueue().queueName,
      MessageBuilder.withPayload(mapper.writeValueAsString(auditEvent)).build()
    )
  }
}

private fun AuditEvent.asMap(): Map<String, String> {
  val items = mutableMapOf("what" to what, "when" to `when`.toString())
  items.addIfNotNull("operationId", operationId)
  items.addIfNotNull("who", who)
  items.addIfNotNull("service", service)
  items.addIfNotNull("details", details)
  return items.toMap()
}

fun MutableMap<String, String>.addIfNotNull(key: String, value: String?) {
  value?.let { this.put(key, value) }
}
