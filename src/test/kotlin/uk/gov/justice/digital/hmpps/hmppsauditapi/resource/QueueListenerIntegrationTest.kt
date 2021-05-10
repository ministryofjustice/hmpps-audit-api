package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.amazonaws.services.sqs.AmazonSQSAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @Autowired
  protected lateinit var awsSqsClient: AmazonSQSAsync

  @Value("\${sqs.queue.name}")
  protected lateinit var queueName: String

  @SpyBean
  protected lateinit var auditService: AuditService

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueName.queueUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}
