package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.sqs.HmppsQueueService

@ConstructorBinding
@ConfigurationProperties(prefix = "hmpps.sqs")
data class SqsConfigProperties(
  val region: String,
  val provider: String,
  val localstackUrl: String = "",
  val queues: Map<String, QueueConfig>,
) {
  data class QueueConfig(
    val queueName: String,
    val queueAccessKeyId: String = "",
    val queueSecretAccessKey: String = "",
    val dlqName: String,
    val dlqAccessKeyId: String = "",
    val dlqSecretAccessKey: String = "",
  )
}

fun SqsConfigProperties.mainQueue() =
  queues["main"] ?: throw MissingQueueException("main queue has not been loaded from configuration properties")

class MissingQueueException(message: String) : RuntimeException(message)

@Configuration
class SqsConfig {

  @Bean
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "aws")
  fun awsSqsClient(
    sqsConfigProperties: SqsConfigProperties,
    awsSqsDlqClient: AmazonSQS,
    hmppsQueueService: HmppsQueueService
  ): AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder.standard()
      .withCredentials(
        AWSStaticCredentialsProvider(
          BasicAWSCredentials(
            sqsConfigProperties.mainQueue().queueAccessKeyId,
            sqsConfigProperties.mainQueue().queueSecretAccessKey
          )
        )
      )
      .withRegion(sqsConfigProperties.region)
      .build()
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

  @Bean
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "aws")
  fun awsSqsDlqClient(sqsConfigProperties: SqsConfigProperties): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withCredentials(
        AWSStaticCredentialsProvider(
          BasicAWSCredentials(
            sqsConfigProperties.mainQueue().dlqAccessKeyId,
            sqsConfigProperties.mainQueue().dlqSecretAccessKey
          )
        )
      )
      .withRegion(sqsConfigProperties.region)
      .build()

  @Bean
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "aws")
  fun queueMessagingTemplate(amazonSQSAsync: AmazonSQSAsync?): QueueMessagingTemplate? =
    QueueMessagingTemplate(amazonSQSAsync)
}
