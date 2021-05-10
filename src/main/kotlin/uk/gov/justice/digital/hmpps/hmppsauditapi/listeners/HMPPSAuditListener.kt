package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Service
@Profile("!no-queue-listener")
class HMPPSAuditListener(
  private val auditService: AuditService,
  private val mapper: ObjectMapper
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  fun onAuditEvent(message: String) {
    log.debug("Received message $message")
    val auditEvent: AuditEvent = mapper.readValue(message, AuditEvent::class.java)
    auditService.audit(auditEvent)
  }

  @Entity(name = "AuditEvent")
  @Table(name = "AuditEvent")
  data class AuditEvent(
    @Id
    @GeneratedValue
    val id: UUID? = null,
    val what: String,
    @Column(name = "occurred")
    val `when`: Instant = Instant.now(),
    val operationId: String? = null,
    val who: String? = null,
    val service: String? = null,
    val details: String? = null
  )
}
