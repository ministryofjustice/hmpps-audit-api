package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.SqsConfigProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.mainQueue
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

  @Autowired
  protected lateinit var awsSqsDlqClient: AmazonSQS

  @Autowired
  protected lateinit var sqsConfigProperties: SqsConfigProperties

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes =
      awsSqsClient.getQueueAttributes(
        sqsConfigProperties.mainQueue().queueName.queueUrl(),
        listOf("ApproximateNumberOfMessages")
      )
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun getNumberOfMessagesCurrentlyOnDlq(): Int? {
    val queueAttributes =
      awsSqsDlqClient.getQueueAttributes(
        sqsConfigProperties.mainQueue().dlqName.queueUrl(),
        listOf("ApproximateNumberOfMessages")
      )
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}
