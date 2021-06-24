package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.queues

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.mainQueue
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

class PurgeQueueTest : QueueListenerIntegrationTest() {

  @Nested
  inner class PurgeDlq {
    @Test
    fun `should fail if no token`() {
      webTestClient.put()
        .uri("/queue-admin/purge-queue/${sqsConfigProperties.mainQueue().dlqName}")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should fail if wrong role`() {
      webTestClient.put()
        .uri("/queue-admin/purge-queue/${sqsConfigProperties.mainQueue().dlqName}")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should fail if using default rather than custom role`() {
      webTestClient.put()
        .uri("/queue-admin/purge-queue/${sqsConfigProperties.mainQueue().dlqName}")
        .headers(setAuthorisation(roles = listOf("ROLE_QUEUE_ADMIN"), scopes = listOf("write")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should fail it dlq does not exist`() {
      webTestClient.put()
        .uri("/queue-admin/purge-queue/UNKNOWN_DLQ")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT_API_QUEUE_ADMIN"), scopes = listOf("write")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should purge message from the DLQ`() {
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
      await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
      awsSqsDlqClient.sendMessage(sqsConfigProperties.mainQueue().dlqName.queueUrl(), message)
      await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }

      webTestClient.put()
        .uri("/queue-admin/purge-queue/${sqsConfigProperties.mainQueue().dlqName}")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT_API_QUEUE_ADMIN"), scopes = listOf("write")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
      await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
    }
  }
}
