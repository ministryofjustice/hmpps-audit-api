package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.QueueAttributeName
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@Configuration
class LocalStackConfig {

  companion object {
    val logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean("awsSqsClient")
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "localstack")
  fun sqsClient(
    sqsConfigProperties: SqsConfigProperties,
    awsSqsDlqClient: AmazonSQS,
    hmppsQueueService: HmppsQueueService
  ): AmazonSQSAsync =
    amazonSQSAsync(sqsConfigProperties.localstackUrl, sqsConfigProperties.region)
      .also { sqsClient -> createMainQueue(sqsClient, awsSqsDlqClient, sqsConfigProperties) }
      .also { logger.info("Created sqs client for queue ${sqsConfigProperties.mainQueue().queueName}") }
      .also {
        with(sqsConfigProperties.mainQueue()) {
          hmppsQueueService.registerHmppsQueue(
            "main",
            it,
            queueName,
            awsSqsDlqClient,
            dlqName
          )
        }
      }

  @Bean("awsSqsDlqClient")
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "localstack")
  fun sqsDlqClient(sqsConfigProperties: SqsConfigProperties): AmazonSQS =
    amazonSQS(sqsConfigProperties.localstackUrl, sqsConfigProperties.region)
      .also { dlqSqsClient -> dlqSqsClient.createQueue(sqsConfigProperties.mainQueue().dlqName) }
      .also { logger.info("Created dlq sqs client for dlq ${sqsConfigProperties.mainQueue().dlqName}") }

  private fun amazonSQSAsync(serviceEndpoint: String, region: String): AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  private fun amazonSQS(serviceEndpoint: String, region: String): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  private fun createMainQueue(
    queueSqsClient: AmazonSQSAsync,
    dlqSqsClient: AmazonSQS,
    sqsConfigProperties: SqsConfigProperties,
  ) =
    dlqSqsClient.getQueueUrl(sqsConfigProperties.mainQueue().dlqName).queueUrl
      .let { dlqQueueUrl -> dlqSqsClient.getQueueAttributes(dlqQueueUrl, listOf(QueueAttributeName.QueueArn.toString())).attributes["QueueArn"]!! }
      .also { queueArn ->
        queueSqsClient.createQueue(
          CreateQueueRequest(sqsConfigProperties.mainQueue().queueName).withAttributes(
            mapOf(
              QueueAttributeName.RedrivePolicy.toString() to
                """{"deadLetterTargetArn":"$queueArn","maxReceiveCount":"5"}"""
            )
          )
        )
      }

  @Bean("queueMessagingTemplate")
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "localstack")
  fun queueMessagingTemplate(amazonSQSAsync: AmazonSQSAsync?): QueueMessagingTemplate? = QueueMessagingTemplate(amazonSQSAsync)
}
