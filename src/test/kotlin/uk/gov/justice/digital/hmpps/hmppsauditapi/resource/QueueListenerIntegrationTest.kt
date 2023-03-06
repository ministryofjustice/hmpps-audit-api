package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @MockBean
  lateinit var auditRepository: AuditRepository

  @Autowired
  protected lateinit var listener: HMPPSAuditListener

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes =
      awsSqsClient.getQueueAttributes(
        awsSqsUrl,
        listOf("ApproximateNumberOfMessages"),
      )
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun getNumberOfMessagesCurrentlyOnDlq(): Int? {
    val queueAttributes =
      awsSqsDlqClient.getQueueAttributes(
        awsSqsDlqUrl,
        listOf("ApproximateNumberOfMessages"),
      )
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}
