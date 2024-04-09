package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.UserService
import java.util.UUID

internal class HMPPSUserListenerTest : QueueListenerIntegrationTest() {

  @Autowired
  private lateinit var userListener: HMPPSUserListener

  @SpyBean
  private lateinit var userService: UserService

  @Test
  internal fun `will call service for a user creation event`() {
    val message = """
    {
      "eventType": "CREATE_USER",
      "userUuid": "b1f4b9cf-1017-491f-82b5-b153ebcda33b",
      "userId": "1234567890",
      "username": "jsmith",
      "emailAddress": "john.smith@domain.com"
    }
  """
    userListener.onAuditUserEvent(message)

    doNothing().whenever(userService).saveNewUserDetails(any())

    verify(userService).saveNewUserDetails(
      check {
        assertThat(it.userUuid).isEqualTo(UUID.fromString("b1f4b9cf-1017-491f-82b5-b153ebcda33b"))
        assertThat(it.userId).isEqualTo("1234567890")
        assertThat(it.username).isEqualTo("jsmith")
        assertThat(it.emailAddress).isEqualTo("john.smith@domain.com")
      },
    )
  }
}
