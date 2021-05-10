package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @MockBean
  lateinit var auditRepository: AuditRepository

  @Autowired
  protected lateinit var listener: HMPPSAuditListener

  @Autowired
  protected lateinit var awsSqsClient: AmazonSQSAsync

  @Value("\${sqs.queue.name}")
  protected lateinit var queueName: String

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueName.queueUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}
