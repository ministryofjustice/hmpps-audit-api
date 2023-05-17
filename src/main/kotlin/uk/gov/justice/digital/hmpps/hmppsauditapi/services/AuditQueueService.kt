package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.SecurityUserContext
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Service
class AuditQueueService(
  hmppsQueueService: HmppsQueueService,
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val securityUserContext: SecurityUserContext,
  private val objectMapper: ObjectMapper,
) {
  private val auditQueue by lazy {
    hmppsQueueService.findByQueueId("auditqueue") ?: throw RuntimeException("Queue with name audit doesn't exist")
  }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }
  private val auditSqsClient by lazy { auditQueue.sqsClient }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendAuditAuditEvent(what: String, details: Any) {
    val auditEvent = AuditEvent(
      what = what,
      who = securityUserContext.principal,
      service = serviceName,
      details = objectMapper.writeValueAsString(details),
    )
    sendAuditEvent(auditEvent)
  }

  fun sendAuditEvent(auditEvent: AuditEvent) {
    log.debug("Audit queue name {} {} {}", auditQueueUrl, auditEvent, auditSqsClient)
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(objectMapper.writeValueAsString(auditEvent))
        .build(),
    )
  }
}

fun MutableMap<String, String>.addIfNotNull(key: String, value: String?) {
  value?.let { this.put(key, value) }
}
