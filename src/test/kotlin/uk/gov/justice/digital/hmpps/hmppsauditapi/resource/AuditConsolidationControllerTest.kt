package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditConsolidationService
import java.time.LocalDate

@SpringBootTest(
  webEnvironment = WebEnvironment.RANDOM_PORT,
)
class AuditConsolidationControllerTest : IntegrationTest() {
  @MockitoBean
  private lateinit var consolidationService: AuditConsolidationService

  private val path = "/internal/integration-test/STAFF/consolidations"
  private val role = listOf("ROLE_AUDIT_INTEGRATION_TEST")

  @Test
  fun `valid POST triggers consolidation job and returns accepted`() {
    val date = LocalDate.of(2025, 1, 1)

    webTestClient.post().uri("$path?date=$date")
      .headers(setAuthorisation(roles = role))
      .exchange()
      .expectStatus().isAccepted
      .expectBody().isEmpty

    verify(consolidationService).startConsolidationJob(AuditEventType.STAFF, date)
  }

  @Test
  fun `POST requires authentication`() {
    webTestClient.post().uri("$path?date=2025-01-01")
      .exchange()
      .expectStatus().isUnauthorized

    verifyNoInteractions(consolidationService)
  }

  @Test
  fun `POST requires the integration test role`() {
    webTestClient.post().uri("$path?date=2025-01-01")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
      .exchange()
      .expectStatus().isForbidden

    verifyNoInteractions(consolidationService)
  }

  @Test
  fun `POST rejects unknown event types and invalid dates`() {
    webTestClient.post().uri("/internal/integration-test/UNKNOWN/consolidations?date=2025-01-01")
      .headers(setAuthorisation(roles = role))
      .exchange()
      .expectStatus().isBadRequest

    listOf("$path", "$path?date=not-a-date", "$path?date=2025-02-30").forEach { uri ->
      webTestClient.post().uri(uri)
        .headers(setAuthorisation(roles = role))
        .exchange()
        .expectStatus().isBadRequest
    }

    verifyNoInteractions(consolidationService)
  }
}
