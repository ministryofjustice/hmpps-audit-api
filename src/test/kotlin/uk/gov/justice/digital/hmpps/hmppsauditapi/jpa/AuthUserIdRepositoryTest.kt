package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserId

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuthUserIdRepositoryTest {

  @Autowired
  private lateinit var authUserIdRepository: AuthUserIdRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuthUserIds by user ID`() {
    // Given
    val userId = "1234567890"
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val authUserId1 = AuthUserId(auditUser = savedAuditUser, userId = userId)
    val authUserId2 = AuthUserId(auditUser = savedAuditUser, userId = userId)
    val authUserId3 = AuthUserId(auditUser = savedAuditUser, userId = "other user ID")

    // When
    authUserIdRepository.save(authUserId1)
    authUserIdRepository.save(authUserId2)
    authUserIdRepository.save(authUserId3)

    // Then
    val foundUserIdes = authUserIdRepository.findAllByUserId(userId)
    assertThat(foundUserIdes).hasSize(2)
      .extracting("userId")
      .containsOnly(userId)
    assertThat(foundUserIdes).allMatch { authUserId ->
      authUserId.auditUser.id == savedAuditUser.id
    }
  }
}
