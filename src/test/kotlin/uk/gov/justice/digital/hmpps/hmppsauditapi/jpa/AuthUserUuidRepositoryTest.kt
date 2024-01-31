package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserUuid
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuthUserUuidRepositoryTest {

  @Autowired
  private lateinit var authUserUuidRepository: AuthUserUuidRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuthUserUuids by userUuid`() {
    // Given
    val userUuid = UUID.fromString("66abbda0-7b64-40a9-ba25-d070ef232a5c")
    val oldUserUuid = UUID.fromString("7438b1b1-26fd-4911-a660-ff949289076e")
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val authUserUuid1 = AuthUserUuid(auditUser = savedAuditUser, userUuid = userUuid)
    val authUserUuid2 = AuthUserUuid(auditUser = savedAuditUser, userUuid = userUuid)
    val authUserUuid3 = AuthUserUuid(auditUser = savedAuditUser, userUuid = oldUserUuid)

    // When
    authUserUuidRepository.save(authUserUuid1)
    authUserUuidRepository.save(authUserUuid2)
    authUserUuidRepository.save(authUserUuid3)

    // Then
    val foundUserUuids = authUserUuidRepository.findAllByUserUuid(userUuid)
    assertThat(foundUserUuids).hasSize(2)
      .extracting("userUuid")
      .containsOnly(userUuid)
    assertThat(foundUserUuids).allMatch { authUserUuid ->
      authUserUuid.auditUser.id == savedAuditUser.id
    }
  }
}
