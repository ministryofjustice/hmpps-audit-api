package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import java.io.IOException
import java.time.Instant
import java.util.UUID

private const val DEFAULT_SUBJECT_TYPE = "NOT_APPLICABLE"

@Service
class HMPPSAuditListener(
  private val auditService: AuditService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("auditqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onAuditEvent(message: String) {
    val patchedJson = patchSubjectTypeIfMissing(message)
    val auditEvent: AuditEvent = objectMapper.readValue(patchedJson, AuditEvent::class.java)

    val cleansedAuditEvent = auditEvent.copy(
      details = auditEvent.details?.jsonString(),
    )

    auditService.audit(cleansedAuditEvent)
  }

  private fun patchSubjectTypeIfMissing(message: String): String = try {
    val tree = jacksonObjectMapper().readTree(message)
    if (!tree.has("subjectType") || tree.get("subjectType").isNull) {
      (tree as ObjectNode).put("subjectType", DEFAULT_SUBJECT_TYPE)
    }

    tree.toString()
  } catch (e: Exception) {
    message
  }

  private fun String.jsonString(): String? = try {
    jacksonObjectMapper().readTree(trim())
    ifBlank { null }
  } catch (e: IOException) {
    "{\"details\":\"$this\"}"
  }

  @Entity(name = "AuditEvent")
  @Table(name = "AuditEvent")
  @Schema(description = "Audit Event Insert Record")
  data class AuditEvent(
    @Id
    @GeneratedValue
    @Hidden
    var id: UUID? = null,
    @Schema(description = "Detailed description of the Event", example = "COURT_REGISTER_BUILDING_UPDATE", required = true)
    val what: String,
    @Column(name = "occurred")
    @Schema(description = "When the Event occurred", example = "2021-04-01T15:15:30Z")
    val `when`: Instant = Instant.now(),
    @Schema(description = "The App Insights operation Id for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
    val operationId: String? = null,
    @Schema(description = "The subject ID for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
    val subjectId: String? = null,
    @Schema(description = "The subject type for the Event", example = "PERSON")
    val subjectType: String = DEFAULT_SUBJECT_TYPE,
    @Schema(description = "The correlation ID for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
    val correlationId: String? = null,
    @Schema(description = "Who initiated the Event", example = "fred.smith@myemail.com")
    val who: String? = null,
    @Schema(description = "Which service the Event relates to", example = "court-register")
    val service: String? = null,
    @Schema(description = "Additional information", example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}")
    val details: String? = null,
  )
}
