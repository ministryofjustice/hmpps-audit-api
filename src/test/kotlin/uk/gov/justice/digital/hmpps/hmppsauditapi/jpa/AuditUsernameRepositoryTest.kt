package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUsername
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuditUsernameRepositoryTest {

  @Autowired
  private lateinit var auditUsernameRepository: AuditUsernameRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuditUsername by username`() {
    // Given
    val username = "jsmith"
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val auditUsername1 = AuditUsername(auditUser = savedAuditUser, username = username)
    val auditUsername2 = AuditUsername(auditUser = savedAuditUser, username = username)
    val auditUsername3 = AuditUsername(auditUser = savedAuditUser, username = "an old username")

    // When
    auditUsernameRepository.save(auditUsername1)
    auditUsernameRepository.save(auditUsername2)
    auditUsernameRepository.save(auditUsername3)

    // Then
    val foundUsernamees = auditUsernameRepository.findAllByUsername(username)
    assertThat(foundUsernamees).hasSize(2)
      .extracting("username")
      .containsOnly(username)
    assertThat(foundUsernamees).allMatch { auditUsername ->
      auditUsername.auditUser.id == savedAuditUser.id
    }
  }
}
