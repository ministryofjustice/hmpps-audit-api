package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsauditapi.exception.UserAlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditUserRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuthEmailAddressRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuthUserIdRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuthUsernameRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthEmailAddress
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserId
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUsername
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSUserListener.UserCreationEvent
import java.util.UUID

private const val AUTH_USER_ID = "the user ID"
private const val AUTH_USERNAME = "testUsername"
private const val AUTH_EMAIL_ADDRESS = "test@example.com"
private val AUDIT_USER_UUID: UUID = UUID.randomUUID()

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

  @Mock
  private lateinit var auditUserRepository: AuditUserRepository

  @Mock
  private lateinit var authUserIdRepository: AuthUserIdRepository

  @Mock
  private lateinit var authEmailAddressRepository: AuthEmailAddressRepository

  @Mock
  private lateinit var authUsernameRepository: AuthUsernameRepository

  private lateinit var userService: UserService

  @Captor
  private lateinit var authUserIdCaptor: ArgumentCaptor<AuthUserId>

  @Captor
  private lateinit var authEmailAddressCaptor: ArgumentCaptor<AuthEmailAddress>

  @Captor
  private lateinit var authUsernameCaptor: ArgumentCaptor<AuthUsername>

  private val newUserDetails = UserCreationEvent(userId = AUTH_USER_ID, username = AUTH_USERNAME, emailAddress = AUTH_EMAIL_ADDRESS)

  @BeforeEach
  fun setUp() {
    userService = UserService(auditUserRepository, authUserIdRepository, authEmailAddressRepository, authUsernameRepository)
  }

  @Test
  fun `saveNewUserDetails should save user details when no existing records found`() {
    given(auditUserRepository.save(any<AuditUser>())).willReturn(AuditUser(id = AUDIT_USER_UUID))
    given(authUserIdRepository.findAllByUserId(AUTH_USER_ID)).willReturn(emptyList())
    given(authEmailAddressRepository.findAllByEmailAddress(AUTH_EMAIL_ADDRESS)).willReturn(emptyList())
    given(authUsernameRepository.findAllByUsername(AUTH_USERNAME)).willReturn(emptyList())

    userService.saveNewUserDetails(newUserDetails)

    verify(authUserIdRepository).save(authUserIdCaptor.capture())
    verify(authEmailAddressRepository).save(authEmailAddressCaptor.capture())
    verify(authUsernameRepository).save(authUsernameCaptor.capture())

    assertThat(authUserIdCaptor.value.auditUser.id).isEqualTo(AUDIT_USER_UUID)
    assertThat(authUserIdCaptor.value.userId).isEqualTo(AUTH_USER_ID)

    assertThat(authEmailAddressCaptor.value.auditUser.id).isEqualTo(AUDIT_USER_UUID)
    assertThat(authEmailAddressCaptor.value.emailAddress).isEqualTo(AUTH_EMAIL_ADDRESS)

    assertThat(authUsernameCaptor.value.auditUser.id).isEqualTo(AUDIT_USER_UUID)
    assertThat(authUsernameCaptor.value.username).isEqualTo(AUTH_USERNAME)
  }

  @Test
  fun `saveNewUserDetails should throw UserAlreadyExistsException when user ID already exists`() {
    given(authUserIdRepository.findAllByUserId(newUserDetails.userId)).willReturn(listOf(AuthUserId(userId = AUTH_USER_ID, auditUser = AuditUser())))

    assertThatThrownBy { userService.saveNewUserDetails(newUserDetails) }
      .isInstanceOf(UserAlreadyExistsException::class.java)
      .hasMessageContaining("User with user ID ${newUserDetails.userId} already exists")
  }

  @Test
  fun `saveNewUserDetails should throw UserAlreadyExistsException when user email address already exists`() {
    given(authEmailAddressRepository.findAllByEmailAddress(newUserDetails.emailAddress)).willReturn(listOf(AuthEmailAddress(emailAddress = AUTH_EMAIL_ADDRESS, auditUser = AuditUser())))

    assertThatThrownBy { userService.saveNewUserDetails(newUserDetails) }
      .isInstanceOf(UserAlreadyExistsException::class.java)
      .hasMessageContaining("User with email address ${newUserDetails.emailAddress} already exists")
  }

  @Test
  fun `saveNewUserDetails should throw UserAlreadyExistsException when username already exists`() {
    given(authUsernameRepository.findAllByUsername(newUserDetails.username)).willReturn(listOf(AuthUsername(username = AUTH_USERNAME, auditUser = AuditUser())))

    assertThatThrownBy { userService.saveNewUserDetails(newUserDetails) }
      .isInstanceOf(UserAlreadyExistsException::class.java)
      .hasMessageContaining("User with username ${newUserDetails.username} already exists")
  }
}
