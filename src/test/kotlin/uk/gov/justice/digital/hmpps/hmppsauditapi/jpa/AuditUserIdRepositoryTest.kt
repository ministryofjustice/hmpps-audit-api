package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUserId

@ExtendWith(SpringExtension::class)
@DataJpaTest
class AuditUserIdRepositoryTest {

  @Autowired
  private lateinit var auditUserIdRepository: AuditUserIdRepository

  @Autowired
  private lateinit var auditUserRepository: AuditUserRepository

  @Test
  fun `should find all AuditUserId by user ID`() {
    // Given
    val userId = "1234567890"
    val savedAuditUser = auditUserRepository.save(AuditUser())
    val auditUserId1 = AuditUserId(auditUser = savedAuditUser, userId = userId)
    val auditUserId2 = AuditUserId(auditUser = savedAuditUser, userId = userId)
    val auditUserId3 = AuditUserId(auditUser = savedAuditUser, userId = "other user ID")

    // When
    auditUserIdRepository.save(auditUserId1)
    auditUserIdRepository.save(auditUserId2)
    auditUserIdRepository.save(auditUserId3)

    // Then
    val foundUserIdes = auditUserIdRepository.findAllByUserId(userId)
    assertThat(foundUserIdes).hasSize(2)
      .extracting("userId")
      .containsOnly(userId)
    assertThat(foundUserIdes).allMatch { auditUserId ->
      auditUserId.auditUser.id == savedAuditUser.id
    }
  }
}
