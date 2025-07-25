package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTest() {

  @Test
  fun `Health page reports ok`() {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Queue health reports audit queue details`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.auditqueue-health.details.queueName").isEqualTo(auditQueueConfig.queueName)
      .jsonPath("components.auditqueue-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.auditqueue-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.auditqueue-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.auditqueue-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.auditqueue-health.details.dlqName").isEqualTo(auditQueueConfig.dlqName!!)
  }

  @Test
  fun `Queue health reports prisoner audit queue details`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.prisonerauditqueue-health.details.queueName").isEqualTo(prisonerAuditQueueConfig.queueName)
      .jsonPath("components.prisonerauditqueue-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.prisonerauditqueue-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.prisonerauditqueue-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.prisonerauditqueue-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.prisonerauditqueue-health.details.dlqName").isEqualTo(prisonerAuditQueueConfig.dlqName!!)
  }

  @Test
  fun `Queue health reports new user queue details`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.auditusersqueue-health.details.queueName").isEqualTo(auditUsersQueueConfig.queueName)
      .jsonPath("components.auditusersqueue-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.auditusersqueue-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.auditusersqueue-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.auditusersqueue-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.auditusersqueue-health.details.dlqName").isEqualTo(auditUsersQueueConfig.dlqName!!)
  }

  @Test
  fun `Health info reports version`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        },
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `db reports ok`() {
    webTestClient.get()
      .uri("/health/db")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}
