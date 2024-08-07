package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.support.DefaultActiveProfilesResolver
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditS3Client

@TestPropertySource(properties = ["hmpps.repository.saveToS3Bucket=true"])
@ActiveProfiles(resolver = DefaultActiveProfilesResolver::class, inheritProfiles = false)
class AuditTestS3Bucket @Autowired constructor(
  override var webTestClient: WebTestClient,
) : QueueListenerIntegrationTest() {

  private val basicAuditEvent = HMPPSAuditListener.AuditEvent(what = "basicAuditEvent", service = "hmpps-audit-poc-ui")

  @SpyBean
  private lateinit var auditS3Client: AuditS3Client

  @Test
  fun `save basic audit entry to S3 bucket`() {
    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(basicAuditEvent))
      .exchange()
      .expectStatus().isAccepted

    await untilCallTo { mockingDetails(auditS3Client).invocations.size } matches { it == 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(auditS3Client).save(any<HMPPSAuditListener.AuditEvent>())
    verifyNoInteractions(auditRepository)
  }
}
