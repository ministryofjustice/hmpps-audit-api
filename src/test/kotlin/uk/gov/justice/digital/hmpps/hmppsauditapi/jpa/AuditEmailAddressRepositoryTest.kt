package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditEmailAddress
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuditEmailAddressRepositoryTest {

  @Autowired
  private lateinit var auditEmailAddressRepository: AuditEmailAddressRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuditEmailAddress by email address`() {
    // Given
    val email = "test@example.com"
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val auditEmailAddress1 = AuditEmailAddress(auditUser = savedAuditUser, emailAddress = email)
    val auditEmailAddress2 = AuditEmailAddress(auditUser = savedAuditUser, emailAddress = email)
    val auditEmailAddress3 = AuditEmailAddress(auditUser = savedAuditUser, emailAddress = "other@example.com")

    // When
    auditEmailAddressRepository.save(auditEmailAddress1)
    auditEmailAddressRepository.save(auditEmailAddress2)
    auditEmailAddressRepository.save(auditEmailAddress3)

    // Then
    val foundEmailAddresses = auditEmailAddressRepository.findAllByEmailAddress(email)
    assertThat(foundEmailAddresses).hasSize(2)
      .extracting("emailAddress")
      .containsOnly(email)
    assertThat(foundEmailAddresses).allMatch { auditEmailAddress ->
      auditEmailAddress.auditUser.id.toString() == savedAuditUser.id.toString()
    }
  }
}
