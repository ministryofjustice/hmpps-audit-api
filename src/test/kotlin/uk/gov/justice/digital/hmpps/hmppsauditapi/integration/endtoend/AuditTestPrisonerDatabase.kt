package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

@TestPropertySource(properties = ["hmpps.repository.saveToS3Bucket=false"])
class AuditTestPrisonerDatabase @Autowired constructor(
  override var webTestClient: WebTestClient,
) : QueueListenerIntegrationTest() {

  private val basicAuditEvent = AuditEvent(what = "basicPrisonerAuditEvent", service = "hmpps-Launchpad-ui")

  @Test
  fun `will consume an audit event message`() {
    val message = """
    {
      "what": "VISITS_VIEWED",
      "when": "2021-01-25T12:30:00Z",
      "operationId": "badea6d876c62e2f5264c94c7b50875e",
      "subjectId": "y1dea6d876c62e2f5264c94c7b50875r",
      "subjectType": "PERSON",
      "who": "bobby.beans",
      "service": "hmpps-Launchpad-ui",
      "details": "{ \"VISITS\": \"9\"}"
    }
    """

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(awsSqsPrisonerAuditUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    verify(telemetryClient).trackEvent(
      eq("hmpps-prisoner-audit"),
      check {
        assertThat(it["what"]).isEqualTo("VISITS_VIEWED")
        assertThat(it["when"]).isEqualTo("2021-01-25T12:30:00Z")
        assertThat(it["operationId"]).isEqualTo("badea6d876c62e2f5264c94c7b50875e")
        assertThat(it["who"]).isEqualTo("bobby.beans")
        assertThat(it["service"]).isEqualTo("hmpps-Launchpad-ui")
        assertThat(it.containsKey("details")).isEqualTo(false)
      },
      isNull(),
    )
    verify(prisonerAuditRepository).save(any<PrisonerAuditEvent>())
  }
}
