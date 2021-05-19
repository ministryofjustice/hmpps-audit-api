package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.time.Instant
import java.time.temporal.ChronoUnit

@DataJpaTest
class AuditRepositoryTest {

  @Autowired
  lateinit var auditRepository: AuditRepository

  @AfterEach
  fun `remove test audit events`() {
    auditRepository.deleteAll()
  }

  @Suppress("ClassName")
  @Nested
  inner class saveAuditEvents {
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

  @Suppress("ClassName")
  @Nested
  inner class pagedAuditEvents {
    @Test
    internal fun `find paged audit events`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          who = "John Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          who = "Fred Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          who = "John Smith",
        )
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val firstPage = auditRepository.findPage(PageRequest.of(0, 2), null, null)
      assertThat(firstPage.numberOfElements).isEqualTo(2)

      val secondPage = auditRepository.findPage(PageRequest.of(1, 2), null, null)
      assertThat(secondPage.numberOfElements).isEqualTo(1)
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class filteredAuditEvents {
    @Test
    internal fun `filter audit events by who`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          who = "John Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          who = "Fred Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          who = "John Smith",
        )
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(Pageable.unpaged(), "John Smith", null)
      assertThat(auditEvents.totalElements).isEqualTo(2)
    }

    @Test
    internal fun `filter audit events by who when no match`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          who = "John Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          who = "Fred Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          who = "John Smith",
        )
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(Pageable.unpaged(), "No match", null)
      assertThat(auditEvents.totalElements).isEqualTo(0)
    }

    @Test
    internal fun `filter audit events by what`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          who = "John Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          who = "Fred Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          who = "John Smith",
        )
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(Pageable.unpaged(), what = "Another Event", who = null)
      assertThat(auditEvents.totalElements).isEqualTo(1)
    }

    @Test
    internal fun `filter audit events by what when no match`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          who = "John Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          who = "Fred Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          who = "John Smith",
        )
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(Pageable.unpaged(), null, "No match")
      assertThat(auditEvents.totalElements).isEqualTo(0)
    }

    @Test
    internal fun `filter audit events by what and who`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          who = "John Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          who = "Fred Smith",
        )
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          who = "John Smith",
        )
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(Pageable.unpaged(), "John Smith", "Another Event By John")
      assertThat(auditEvents.totalElements).isEqualTo(1)
    }
  }
}
