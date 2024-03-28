package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.UserService
import java.util.UUID

@Service
class HMPPSUserListener(
  private val userService: UserService,
  private val objectMapper: ObjectMapper,
) {

  @SqsListener("newuserqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onUserCreation(message: String) {
    val userCreationEvent: UserCreationEvent = objectMapper.readValue(message, UserCreationEvent::class.java)
    userService.saveNewUserDetails(userCreationEvent)
  }

  data class UserCreationEvent(
    val userUuid: UUID,
    val userId: String,
    val username: String,
    val emailAddress: String,
  )
}
