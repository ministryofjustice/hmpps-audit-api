package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

@TestPropertySource(properties = ["hmpps.repository.saveToS3Bucket=true"])
@ActiveProfiles(resolver = CommandLineProfilesResolver::class, inheritProfiles = false)
class AuditTestS3Bucket @Autowired constructor(
  override var webTestClient: WebTestClient,
) : QueueListenerIntegrationTest() {

  private val basicAuditEvent = AuditEvent(what = "basicAuditEvent", service = "hmpps-audit-poc-ui")

  @Test
  fun `save basic audit entry to S3 bucket`() {
    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(basicAuditEvent))
      .exchange()
      .expectStatus().isAccepted

    await untilCallTo { mockingDetails(auditS3Client).invocations.size } matches { it!! >= 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(auditS3Client).save(
      argThat {
        what == "basicAuditEvent" &&
          service == "hmpps-audit-poc-ui" &&
          subjectType == "NOT_APPLICABLE" &&
          id != null
      },
      eq(staffAthenaProperties.s3BucketName),
    )

    verify(staffAuditRepository).save(
      argThat {
        what == "basicAuditEvent" &&
          service == "hmpps-audit-poc-ui" &&
          subjectType == "NOT_APPLICABLE" &&
          id != null
      },
    )
    verifyNoInteractions(prisonerAuditRepository)
  }
}
