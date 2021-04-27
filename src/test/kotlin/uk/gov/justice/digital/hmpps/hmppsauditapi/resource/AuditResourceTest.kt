package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditResourceTest : IntegrationTest() {

  @Test internal fun `requires a valid authentication token`() {
    webTestClient.get()
      .uri("/audit")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  internal fun `requires the correct role`() {
    webTestClient.get()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  internal fun `get - satisfies the correct role`() {
    webTestClient.get()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
      .exchange()
      .expectStatus().isOk
  }
}
