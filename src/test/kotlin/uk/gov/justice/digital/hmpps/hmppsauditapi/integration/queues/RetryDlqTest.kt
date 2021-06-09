package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.queues

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

class RetryDlqTest : QueueListenerIntegrationTest() {

  @Test
  fun `will process message from the DLQ`() {
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
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
    awsSqsDlqClient.sendMessage(sqsConfigProperties.dlqName.queueUrl(), message)
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }

    webTestClient.put()
      .uri("/queue-admin/retry-dlq/${sqsConfigProperties.dlqName}")
//      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    verify(auditRepository).save(any<AuditEvent>())
  }
}
