package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.SqsConfigProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.mainQueue
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.IntegrationTest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : IntegrationTest() {

  @Autowired
  private lateinit var sqsConfigProperties: SqsConfigProperties

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
  fun `Queue health reports queue details`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.main-health.details.queueName").isEqualTo(sqsConfigProperties.mainQueue().queueName)
      .jsonPath("components.main-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.main-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.main-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.main-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.main-health.details.dlqName").isEqualTo(sqsConfigProperties.mainQueue().dlqName)
  }

  @Test
  fun `Health info reports version`() {
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        }
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
