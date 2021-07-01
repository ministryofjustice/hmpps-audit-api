package uk.gov.justice.digital.hmpps.hmppsauditapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import uk.gov.justice.hmpps.sqs.HmppsQueueProperties
import uk.gov.justice.hmpps.sqs.HmppsQueueProperties.QueueConfig
import java.time.Instant
import java.util.UUID

class AuditServiceTest {
  private val telemetryClient: TelemetryClient = mock()
  private val auditRepository: AuditRepository = mock()
  private val hmppsQueueProperties = HmppsQueueProperties(
    region = "eu-west-2",
    provider = "aws",
    queues = mapOf("auditQueue" to QueueConfig(queueName = "hmpps-audit-queue", dlqName = "hmpps-audit-dlq"))
  )
  private val auditService =
    AuditService(telemetryClient, auditRepository, jacksonObjectMapper(), mock())

  @Nested
  @Suppress("ClassName")
  inner class findAuditEvents {

    @Test
    fun `find all audit events`() {
      val listOfAudits = listOf(
        AuditEvent(
          UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
          "MINIMUM_FIELDS_EVENT",
          Instant.parse("2021-04-04T17:17:30Z")
        ),
        AuditEvent(
          UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
          "COURT_REGISTER_BUILDING_UPDATE",
          Instant.parse("2021-04-03T10:15:30Z"),
          "badea6d876c62e2f5264c94c7b50875e",
          "bobby.beans",
          "court-register",
          "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
        ),
        AuditEvent(
          UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
          "OFFENDER_DELETED",
          Instant.parse("2021-04-01T15:15:30Z"),
          "cadea6d876c62e2f5264c94c7b50875e",
          "bobby.beans",
          "offender-service",
          "{\"offenderId\": \"97\"}"
        ),
        AuditEvent(
          UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
          "OFFENDER_DELETED",
          Instant.parse("2020-12-31T08:11:30Z"),
          "dadea6d876c62e2f5264c94c7b50875e",
          "freddy.frog",
          "offender-service",
          "{\"offenderId\": \"98\"}"
        )
      )
      whenever(auditRepository.findAll(any<Sort>())).thenReturn(
        listOfAudits
      )

      val audits = auditService.findAll()
      assertThat(audits).isEqualTo(
        listOf(
          AuditDto(
            UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
            "MINIMUM_FIELDS_EVENT",
            Instant.parse("2021-04-04T17:17:30Z"),
            null, null, null, null
          ),
          AuditDto(
            UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
            "COURT_REGISTER_BUILDING_UPDATE",
            Instant.parse("2021-04-03T10:15:30Z"),
            "badea6d876c62e2f5264c94c7b50875e",
            "bobby.beans",
            "court-register",
            "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
          ),
          AuditDto(
            UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
            "OFFENDER_DELETED",
            Instant.parse("2021-04-01T15:15:30Z"),
            "cadea6d876c62e2f5264c94c7b50875e",
            "bobby.beans",
            "offender-service",
            "{\"offenderId\": \"97\"}"
          ),
          AuditDto(
            UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
            "OFFENDER_DELETED",
            Instant.parse("2020-12-31T08:11:30Z"),
            "dadea6d876c62e2f5264c94c7b50875e",
            "freddy.frog",
            "offender-service",
            "{\"offenderId\": \"98\"}"
          )
        )
      )
    }

    @Nested
    inner class findPagedAuditEvents {

      @Test
      fun `find all paged audit events`() {
        val listOfAudits = PageImpl(
          listOf(
            AuditEvent(
              UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
              "MINIMUM_FIELDS_EVENT",
              Instant.parse("2021-04-04T17:17:30Z")
            ),
            AuditEvent(
              UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
              "COURT_REGISTER_BUILDING_UPDATE",
              Instant.parse("2021-04-03T10:15:30Z"),
              "badea6d876c62e2f5264c94c7b50875e",
              "bobby.beans",
              "court-register",
              "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
            ),
            AuditEvent(
              UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
              "OFFENDER_DELETED",
              Instant.parse("2021-04-01T15:15:30Z"),
              "cadea6d876c62e2f5264c94c7b50875e",
              "bobby.beans",
              "offender-service",
              "{\"offenderId\": \"97\"}"
            ),
            AuditEvent(
              UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
              "OFFENDER_DELETED",
              Instant.parse("2020-12-31T08:11:30Z"),
              "dadea6d876c62e2f5264c94c7b50875e",
              "freddy.frog",
              "offender-service",
              "{\"offenderId\": \"98\"}"
            )
          )
        )
        whenever(auditRepository.findPage(any(), anyOrNull(), anyOrNull())).thenReturn(
          listOfAudits
        )

        val audits = auditService.findPage(Pageable.unpaged(), null, null)
        assertThat(audits).isEqualTo(
          PageImpl(
            listOf(
              AuditDto(
                UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
                "MINIMUM_FIELDS_EVENT",
                Instant.parse("2021-04-04T17:17:30Z"),
                null, null, null, null
              ),
              AuditDto(
                UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
                "COURT_REGISTER_BUILDING_UPDATE",
                Instant.parse("2021-04-03T10:15:30Z"),
                "badea6d876c62e2f5264c94c7b50875e",
                "bobby.beans",
                "court-register",
                "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
              ),
              AuditDto(
                UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
                "OFFENDER_DELETED",
                Instant.parse("2021-04-01T15:15:30Z"),
                "cadea6d876c62e2f5264c94c7b50875e",
                "bobby.beans",
                "offender-service",
                "{\"offenderId\": \"97\"}"
              ),
              AuditDto(
                UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
                "OFFENDER_DELETED",
                Instant.parse("2020-12-31T08:11:30Z"),
                "dadea6d876c62e2f5264c94c7b50875e",
                "freddy.frog",
                "offender-service",
                "{\"offenderId\": \"98\"}"
              )
            )
          )
        )
      }
    }
  }
}
