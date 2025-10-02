package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.StaffAuditEvent
import java.time.Instant
import java.time.temporal.ChronoUnit

@DataJpaTest
class StaffAuditRepositoryTest {

  @Autowired
  lateinit var staffAuditRepository: StaffAuditRepository

  @AfterEach
  fun `remove test audit events`() {
    staffAuditRepository.deleteAll()
  }

  @Suppress("ClassName")
  @Nested
  inner class saveAuditEvents {
    @Test
    internal fun `can write to repository with basic attributes`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event with basic attributes",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(1)
      val auditEvent = staffAuditRepository.findAll().first()

      assertThat(auditEvent.id).isNotNull
      assertThat(auditEvent.what).isEqualTo("An Event with basic attributes")
      assertThat(auditEvent.`when`).isNotNull
      assertThat(auditEvent.operationId).isNull()
      assertThat(auditEvent.who).isNull()
      assertThat(auditEvent.service).isNull()
      assertThat(auditEvent.subjectId).isNull()
      assertThat(auditEvent.subjectType).isEqualTo("NOT_APPLICABLE")
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
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event with all attributes",
          `when` = now,
          operationId = "123456789",
          who = "John Smith",
          service = "current-service",
          subjectId = "11111111",
          subjectType = "PERSON",
          details = details,
        ),
      )
      assertThat(staffAuditRepository.count()).isEqualTo(1)
      val auditEvent = staffAuditRepository.findAll().first()
      assertThat(auditEvent.id).isNotNull
      assertThat(auditEvent.what).isEqualTo("An Event with all attributes")
      assertThat(auditEvent.`when`).isCloseTo(now, within(1, ChronoUnit.MICROS))
      assertThat(auditEvent.operationId).isEqualTo("123456789")
      assertThat(auditEvent.who).isEqualTo("John Smith")
      assertThat(auditEvent.service).isEqualTo("current-service")
      assertThat(auditEvent.subjectId).isEqualTo("11111111")
      assertThat(auditEvent.subjectType).isEqualTo("PERSON")
      assertThat(auditEvent.details).isEqualTo(details)
    }
  }

  @Suppress("ClassName")
  @Nested
  inner class filteredPageAuditEvents {
    @Test
    internal fun `filter audit events by date range, service, what and who`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event by John S",
          `when` = Instant.now(),
          operationId = "123456789",
          subjectId = "111",
          subjectType = "PERSON",
          who = "John Smith",
          service = "Service-A",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By Fred S",
          `when` = Instant.now(),
          operationId = "345678",
          subjectId = "222",
          subjectType = "PERSON",
          who = "Fred Smith",
          service = "Service-B",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Event By John J",
          `when` = Instant.now(),
          operationId = "234567",
          subjectId = "333",
          subjectType = "PERSON",
          who = "John Jones",
          service = "Service-C",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        Instant.now().minus(1, ChronoUnit.DAYS),
        Instant.now().plus(1, ChronoUnit.DAYS),
        service = "Service-B",
        what = "Another Event By Fred S",
        who = "Fred Smith",
        subjectType = null,
        subjectId = null,
        correlationId = null,
      )
      assertThat(auditEvents.size).isEqualTo(1)
    }

    @Test
    internal fun `filter audit events by service`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        "Service-B",
        null,
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(1)
    }

    @Test
    internal fun `filter audit events by all null parameters`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
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
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
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
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)

      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        Instant.now().minus(3, ChronoUnit.DAYS),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(3)
    }

    @Test
    internal fun `filter audit events by end date`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        Instant.now().minus(1, ChronoUnit.DAYS),
        null,
        null,
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(2)
    }

    @Test
    internal fun `filter audit events by date range`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)

      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        Instant.now().minus(3, ChronoUnit.DAYS),
        Instant.now(),
        null,
        null,
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(3)
    }

    @Test
    internal fun `filter audit events by date range, where start date is greater than end date, no results expected`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now().minus(1, ChronoUnit.DAYS),
          operationId = "345678",
          service = "Service-B",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now().minus(2, ChronoUnit.DAYS),
          operationId = "234567",
          service = "Service-C",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)

      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        Instant.now(),
        Instant.now().minus(3, ChronoUnit.DAYS),
        null,
        null,
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(0)
    }

    @Test
    internal fun `filter audit events by subjectId`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          subjectId = "11111",
          subjectType = "PERSON",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          subjectId = "22222",
          subjectType = "PERSON",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          subjectId = "33333",
          subjectType = "PERSON",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
        "11111",
        null,
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(1)
      assertThat(auditEvents.first().subjectId).isEqualTo("11111")
    }

    @Test
    internal fun `filter audit events by subjectType`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          operationId = "123456789",
          service = "Service-A",
          subjectId = "11111",
          subjectType = "PERSON",
          who = "John Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          operationId = "345678",
          service = "Service-B",
          subjectId = "22222",
          subjectType = "PERSON",
          who = "Fred Smith",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          operationId = "234567",
          service = "Service-C",
          subjectId = "33333",
          subjectType = "SYSTEM",
          who = "John Smith",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
        null,
        "PERSON",
        null,
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(2)
      assertThat(auditEvents.stream().allMatch { ae -> ae.subjectType == "PERSON" }).isTrue()
    }

    @Test
    internal fun `filter audit events by correlationId`() {
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "An Event",
          `when` = Instant.now(),
          service = "Service-A",
          who = "John Smith",
          correlationId = "correlationId1",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event",
          `when` = Instant.now(),
          service = "Service-B",
          who = "Fred Smith",
          correlationId = "correlationId2",
        ),
      )
      staffAuditRepository.save(
        StaffAuditEvent(
          what = "Another Event By John",
          `when` = Instant.now(),
          who = "John Smith",
          correlationId = "correlationId3",
        ),
      )

      assertThat(staffAuditRepository.count()).isEqualTo(3)
      val auditEvents = staffAuditRepository.findPage(
        Pageable.unpaged(),
        null,
        null,
        null,
        null,
        null,
        "correlationId1",
        null,
        null,
      )
      assertThat(auditEvents.size).isEqualTo(1)
      assertThat(auditEvents.first().correlationId).isEqualTo("correlationId1")
    }
  }
}
