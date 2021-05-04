package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuditRepositoryTest {

  @Autowired
  lateinit var auditRepository: AuditRepository

  @AfterEach
  fun `remove test audit events`() {
    auditRepository.deleteAll()
  }

  @Test
  internal fun `can write to repository with basic attributes`() {
    auditRepository.save(
      AuditEvent(
        what = "An Event with basic attributes"
      )
    )

    assertThat(auditRepository.count()).isEqualTo(1)
    val auditEvent = auditRepository.findAll().first()

    assertThat(auditEvent.id).isNotNull
    assertThat(auditEvent.what).isEqualTo("An Event with basic attributes")
    assertThat(auditEvent.`when`).isNotNull
    assertThat(auditEvent.operationId).isNull()
    assertThat(auditEvent.who).isNull()
    assertThat(auditEvent.service).isNull()
    assertThat(auditEvent.details).isNull()
  }

  @Test
  internal fun `can write to repository with all attributes`() {
    val details = """
      {
        "offenderId": "99"
      }
      """
    val now = Instant.now()
    auditRepository.save(
      AuditEvent(
        what = "An Event with all attributes",
        `when` = now,
        operationId = "123456789",
        who = "John Smith",
        service = "current-service",
        details = details
      )
    )
    assertThat(auditRepository.count()).isEqualTo(1)
    val auditEvent = auditRepository.findAll().first()
    assertThat(auditEvent.id).isNotNull
    assertThat(auditEvent.what).isEqualTo("An Event with all attributes")
    assertThat(auditEvent.`when`).isCloseTo(now, within(1, ChronoUnit.MICROS))
    assertThat(auditEvent.operationId).isEqualTo("123456789")
    assertThat(auditEvent.who).isEqualTo("John Smith")
    assertThat(auditEvent.service).isEqualTo("current-service")
    assertThat(auditEvent.details).isEqualTo(details)
  }
}
