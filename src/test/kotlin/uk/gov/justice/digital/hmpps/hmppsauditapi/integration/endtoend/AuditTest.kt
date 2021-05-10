package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend
import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mockingDetails
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.util.UUID

class AuditTest : QueueListenerIntegrationTest() {
  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @MockBean
  lateinit var auditRepository: AuditRepository

  @Test
  fun `will consume an audit event message`() {
    val message = """
    {
      "what": "OFFENDER_DELETED",
      "when": "2021-01-25T12:30:00Z",
      "operationId": "badea6d876c62e2f5264c94c7b50875e",
      "who": "bobby.beans",
      "service": "offender-service",
      "details": "{ \"offenderId\": \"99\"}"
    }
  """

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueName.queueUrl(), message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    verify(telemetryClient).trackEvent(
      eq("hmpps-audit"),
      check {
        assertThat(it["what"]).isEqualTo("OFFENDER_DELETED")
        assertThat(it["when"]).isEqualTo("2021-01-25T12:30:00Z")
        assertThat(it["operationId"]).isEqualTo("badea6d876c62e2f5264c94c7b50875e")
        assertThat(it["who"]).isEqualTo("bobby.beans")
        assertThat(it["service"]).isEqualTo("offender-service")
        assertThat(it["details"]).isEqualTo("{ \"offenderId\": \"99\"}")
      },
      isNull()
    )
    verify(auditRepository).save(any<AuditEvent>())
  }

  @Suppress("ClassName")
  @Nested
  open inner class insertAuditEntries {
    @Test
    fun `save basic audit entry`() {
      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(AuditEvent(what = "basicAuditEvent")))
        .exchange()
        .expectStatus().isCreated

      AuditService.log.info(auditRepository.hashCode().toString())

      await untilCallTo { mockingDetails(auditRepository).invocations.size } matches { it == 1 }

      verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
      verify(auditRepository).save(any<AuditEvent>())
    }

    @Test
    fun `save full audit entry`() {
      val auditEvent = AuditEvent(
        UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
        "OFFENDER_DELETED",
        Instant.parse("2021-04-01T15:15:30Z"),
        "cadea6d876c62e2f5264c94c7b50875e",
        "bobby.beans",
        "offender-service",
        "{\"offenderId\": \"97\"}"
      )

      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isCreated

      await untilCallTo { mockingDetails(auditRepository).invocations.size } matches { it == 1 }

      verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
      verify(auditRepository).save(auditEvent)
    }
  }
}
