package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUsername

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuthUsernameRepositoryTest {

  @Autowired
  private lateinit var authUsernameRepository: AuthUsernameRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuthUsernames by username`() {
    // Given
    val username = "jsmith"
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val authUsername1 = AuthUsername(auditUser = savedAuditUser, username = username)
    val authUsername2 = AuthUsername(auditUser = savedAuditUser, username = username)
    val authUsername3 = AuthUsername(auditUser = savedAuditUser, username = "an old username")

    // When
    authUsernameRepository.save(authUsername1)
    authUsernameRepository.save(authUsername2)
    authUsernameRepository.save(authUsername3)

    // Then
    val foundUsernames = authUsernameRepository.findAllByUsername(username)
    assertThat(foundUsernames).hasSize(2)
      .extracting("username")
      .containsOnly(username)
    assertThat(foundUsernames).allMatch { authUsername ->
      authUsername.auditUser.id == savedAuditUser.id
    }
  }
}
