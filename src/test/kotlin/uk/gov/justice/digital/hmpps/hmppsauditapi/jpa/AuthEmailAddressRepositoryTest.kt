package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthEmailAddress

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuthEmailAddressRepositoryTest {

  @Autowired
  private lateinit var authEmailAddressRepository: AuthEmailAddressRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuthEmailAddresses by email address`() {
    // Given
    val email = "test@example.com"
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val authEmailAddress1 = AuthEmailAddress(auditUser = savedAuditUser, emailAddress = email)
    val authEmailAddress2 = AuthEmailAddress(auditUser = savedAuditUser, emailAddress = email)
    val authEmailAddress3 = AuthEmailAddress(auditUser = savedAuditUser, emailAddress = "other@example.com")

    // When
    authEmailAddressRepository.save(authEmailAddress1)
    authEmailAddressRepository.save(authEmailAddress2)
    authEmailAddressRepository.save(authEmailAddress3)

    // Then
    val foundEmailAddresses = authEmailAddressRepository.findAllByEmailAddress(email)
    assertThat(foundEmailAddresses).hasSize(2)
      .extracting("emailAddress")
      .containsOnly(email)
    assertThat(foundEmailAddresses).allMatch { authEmailAddress ->
      authEmailAddress.auditUser.id == savedAuditUser.id
    }
  }
}
