package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
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
          what = "An Event with basic attributes",
        ),
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
          details = details,
        ),
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
  inner class filteredPageAuditEvents {
    @Test
    internal fun `filter audit events by date range, service, what and who`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event by John S",
          `when` = Instant.now(),
          operationId = "123456789",
          subjectId = "111",
          subjectType = "PERSON",
          who = "John Smith",
          service = "Service-A",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By Fred S",
          `when` = Instant.now(),
          operationId = "345678",
          subjectId = "222",
          subjectType = "PERSON",
          who = "Fred Smith",
          service = "Service-B",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Event By John J",
          `when` = Instant.now(),
          operationId = "234567",
          subjectId = "333",
          subjectType = "PERSON",
          who = "John Jones",
          service = "Service-C",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        Instant.now().minus(1, ChronoUnit.DAYS),
        Instant.now().plus(1, ChronoUnit.DAYS),
        "Service-B",
        "Another Event By Fred S",
        who = "Fred Smith",
      )
      assertThat(auditEvents.size).isEqualTo(1)
    }

    @Test
    internal fun `filter audit events by service`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        "Service-B",
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(1)
    }

    @Test
    internal fun `filter audit events by all null parameters`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(3)
    }

    @Test
    internal fun `filter audit events by Who`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
        null,
        "John Smith",
      )
      assertThat(auditEvents.size).isEqualTo(2)
    }

    @Test
    internal fun `filter audit events by start date`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)

      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        Instant.now().minus(3, ChronoUnit.DAYS),
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(3)
    }

    @Test
    internal fun `filter audit events by end date`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)
      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        null,
        Instant.now().minus(1, ChronoUnit.DAYS),
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(2)
    }

    @Test
    internal fun `filter audit events by date range`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)

      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        Instant.now().minus(3, ChronoUnit.DAYS),
        Instant.now(),
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(3)
    }

    @Test
    internal fun `filter audit events by date range, where start date is greater than end date, no results expected`() {
      auditRepository.save(
        AuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      auditRepository.save(
        AuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(auditRepository.count()).isEqualTo(3)

      val auditEvents = auditRepository.findPage(
        Pageable.unpaged(),
        Instant.now(),
        Instant.now().minus(3, ChronoUnit.DAYS),
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(0)
    }
  }
}
