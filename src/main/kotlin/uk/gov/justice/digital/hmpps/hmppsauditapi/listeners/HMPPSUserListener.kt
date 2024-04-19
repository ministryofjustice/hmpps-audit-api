package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditUserEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditUserEventType.CREATE_USER
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.UserService

@Service
class HMPPSUserListener(
  private val userService: UserService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("auditusersqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onAuditUserEvent(message: String) {
    val jsonNode: JsonNode = objectMapper.readTree(message)
    val eventType: AuditUserEventType = AuditUserEventType.valueOf(jsonNode.get("eventType").asText())

    when (eventType) {
      CREATE_USER -> {
        val userCreationEvent = objectMapper.treeToValue(jsonNode, UserCreationEvent::class.java)
        userService.saveNewUserDetails(userCreationEvent)
      }
    }
  }

  data class UserCreationEvent(
    val eventType: AuditUserEventType = CREATE_USER,
    val userId: String,
    val username: String,
    val emailAddress: String,
  )
}
