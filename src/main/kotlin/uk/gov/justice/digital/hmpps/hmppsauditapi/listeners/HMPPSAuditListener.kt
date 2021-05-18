package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
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
  @Schema(description = "Audit Event Insert Record")
  data class AuditEvent(
    @Id
    @GeneratedValue
    @Hidden
    val id: UUID? = null,
    @Schema(description = "Detailed description of the Event", example = "COURT_REGISTER_BUILDING_UPDATE", required = true)
    val what: String,
    @Column(name = "occurred")
    @Schema(description = "When the Event occurred", example = "2021-04-01T15:15:30Z")
    val `when`: Instant = Instant.now(),
    @Schema(description = "The App Insights operation Id for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
    var operationId: String? = null,
    @Schema(description = "Who initiated the Event", example = "fred.smith@myemail.com")
    val who: String? = null,
    @Schema(description = "Which service the Event relates to", example = "court-register")
    val service: String? = null,
    @Schema(description = "Additional information", example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}")
    val details: String? = null
  )
}
