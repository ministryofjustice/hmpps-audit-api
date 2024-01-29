package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditEmailAddress
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuditEmailAddressRepositoryTest {

  @Autowired
  private lateinit var entityManager: TestEntityManager

  @Autowired
  private lateinit var auditEmailAddressRepository: AuditEmailAddressRepository

  @Test
  fun `should find all AuditEmailAddress by email address`() {
    val email = "test@example.com"
    val auditUser = AuditUser()
    val auditEmailAddress1 = AuditEmailAddress(auditUser = auditUser, emailAddress = email)
    val auditEmailAddress2 = AuditEmailAddress(auditUser = auditUser, emailAddress = email)
    val auditEmailAddress3 = AuditEmailAddress(auditUser = auditUser, emailAddress = "other@example.com")

    entityManager.persistAndFlush(auditEmailAddress1)
    entityManager.persistAndFlush(auditEmailAddress2)
    entityManager.persistAndFlush(auditEmailAddress3)

    val foundEmailAddresses = auditEmailAddressRepository.findAllByEmailAddress(email)

    assertThat(foundEmailAddresses).hasSize(2)
      .extracting("emailAddress")
      .containsOnly(email)
  }
}
