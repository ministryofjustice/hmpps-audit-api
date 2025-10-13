package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditServiceFactory
import java.io.IOException

private const val DEFAULT_SUBJECT_TYPE = "NOT_APPLICABLE"

@Service
class HMPPSAuditListener(
  private val auditServiceFactory: AuditServiceFactory,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("auditqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onAuditEvent(message: String) {
    val patchedJson = patchSubjectTypeIfMissing(message)
    val auditEvent: AuditEvent = objectMapper.readValue(patchedJson, AuditEvent::class.java)

    val cleansedAuditEvent = auditEvent.copy(
      details = auditEvent.details?.jsonString(),
    )
    auditServiceFactory.getAuditService(AuditEventType.STAFF).saveAuditEvent(cleansedAuditEvent)
  }

  @SqsListener("prisonerauditqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onPrisonerAuditEvent(message: String) {
    val patchedJson = patchSubjectTypeIfMissing(message)
    val auditEvent: AuditEvent = objectMapper.readValue(patchedJson, AuditEvent::class.java)

    val cleansedAuditEvent = auditEvent.copy(
      details = auditEvent.details?.jsonString(),
    )

    auditServiceFactory.getAuditService(AuditEventType.PRISONER).saveAuditEvent(cleansedAuditEvent)
  }

  private fun patchSubjectTypeIfMissing(message: String): String = try {
    val tree = jacksonObjectMapper().readTree(message)
    if (!tree.has("subjectType") || tree.get("subjectType").isNull) {
      (tree as ObjectNode).put("subjectType", DEFAULT_SUBJECT_TYPE)
    }

    tree.toString()
  } catch (_: Exception) {
    message
  }

  private fun String.jsonString(): String? = try {
    jacksonObjectMapper().readTree(trim())
    ifBlank { null }
  } catch (e: IOException) {
    "{\"details\":\"$this\"}"
  }
}
