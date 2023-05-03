package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.util.UUID

@Service
class HMPPSAuditListener(
  private val auditService: AuditService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("auditqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onAuditEvent(message: String) {
    val auditEvent: AuditEvent = objectMapper.readValue(message, AuditEvent::class.java)
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
    val operationId: String? = null,
    @Schema(description = "Who initiated the Event", example = "fred.smith@myemail.com")
    val who: String? = null,
    @Schema(description = "Which service the Event relates to", example = "court-register")
    val service: String? = null,
    @Schema(description = "Additional information", example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}")
    val details: String? = null,
  )
}
