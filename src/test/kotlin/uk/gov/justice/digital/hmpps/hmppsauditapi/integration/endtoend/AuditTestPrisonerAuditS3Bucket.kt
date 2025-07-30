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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

@TestPropertySource(properties = ["hmpps.repository.saveToS3Bucket=true"])
@ActiveProfiles(resolver = CommandLineProfilesResolver::class, inheritProfiles = false)
class AuditTestPrisonerAuditS3Bucket @Autowired constructor(
  override var webTestClient: WebTestClient,
) : QueueListenerIntegrationTest() {

  private val prisonerAuditEvent = AuditEvent(
    id = null,
    what = "prisonerAuditEvent",
    java.time.Instant.parse("2021-04-01T15:15:30Z"),
    operationId = null,
    subjectId = null,
    subjectType = "NOT_APPLICABLE",
    correlationId = null, who = null,
    service = "hmpps-launchpad",
    details = null,
  )

  @Test
  fun `save basic audit entry to S3 bucket`() {
    auditQueueService.sendPrisonerAuditEvent(prisonerAuditEvent)

    await untilCallTo { mockingDetails(auditS3Client).invocations.size } matches { it!! >= 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-prisoner-audit"), any(), isNull())
    verify(auditS3Client).save(
      argThat {
        what == "prisonerAuditEvent" &&
          service == "hmpps-launchpad" &&
          subjectType == "NOT_APPLICABLE" &&
          id != null
      },
      eq("hmpps-prisoner-audit-bucket"),
    )
    verifyNoInteractions(auditRepository)
  }
}
