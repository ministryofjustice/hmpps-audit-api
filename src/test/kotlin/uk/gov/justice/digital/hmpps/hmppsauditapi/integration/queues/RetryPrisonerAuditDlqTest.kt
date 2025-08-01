package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.queues

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

@TestPropertySource(properties = ["hmpps.repository.saveToS3Bucket=true"])
class RetryPrisonerAuditDlqTest : QueueListenerIntegrationTest() {

  @Nested
  inner class RetryDlq {
    @Test
    fun `should fail if no token`() {
      webTestClient.put()
        .uri("/queue-admin/retry-dlq/${prisonerAuditQueueConfig.dlqName}")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should fail if wrong role`() {
      webTestClient.put()
        .uri("/queue-admin/retry-dlq/${prisonerAuditQueueConfig.dlqName}")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should fail if using default rather than custom role`() {
      webTestClient.put()
        .uri("/queue-admin/retry-dlq/${prisonerAuditQueueConfig.dlqName}")
        .headers(setAuthorisation(roles = listOf("ROLE_QUEUE_ADMIN")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should fail it dlq does not exist`() {
      webTestClient.put()
        .uri("/queue-admin/retry-dlq/UNKNOWN_DLQ")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT_API_QUEUE_ADMIN"), scopes = listOf("write")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should process message from the DLQ`() {
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
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditQueue() } matches { it == 0 }
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditDlq() } matches { it == 0 }
      awsSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(awsSqsPrisonerAuditDlqUrl).messageBody(message).build(),
      )
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditDlq() } matches { it == 1 }

      webTestClient.put()
        .uri("/queue-admin/retry-dlq/${prisonerAuditQueueConfig.dlqName}")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT_API_QUEUE_ADMIN"), scopes = listOf("write")))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditDlq() } matches { it == 0 }
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditQueue() } matches { it == 0 }

      verify(auditS3Client).save(any<AuditEvent>(), any())
    }
  }

  @Nested
  inner class RetryAllDlqs {

    @Test
    fun `should process message from the DLQ`() {
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
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditQueue() } matches { it == 0 }
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditDlq() } matches { it == 0 }
      awsSqsPrisonerAuditDlqClient.sendMessage(
        SendMessageRequest.builder().queueUrl(awsSqsPrisonerAuditDlqUrl).messageBody(message).build(),
      )
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditDlq() } matches { it == 1 }

      webTestClient.put()
        .uri("/queue-admin/retry-all-dlqs")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditDlq() } matches { it == 0 }
      await untilCallTo { getNumberOfMessagesCurrentlyOnPrisonerAuditQueue() } matches { it == 0 }

      verify(auditS3Client).save(any<AuditEvent>(), any())
    }
  }
}
