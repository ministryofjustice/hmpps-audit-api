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
  val queueName: String,
  val queueAccessKeyId: String = "",
  val queueSecretAccessKey: String = "",
  val dlqName: String,
  val dlqAccessKeyId: String = "",
  val dlqSecretAccessKey: String = "",
)

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
            sqsConfigProperties.queueAccessKeyId,
            sqsConfigProperties.queueSecretAccessKey
          )
        )
      )
      .withRegion(sqsConfigProperties.region)
      .build()
      .also {
        with(sqsConfigProperties) { hmppsQueueService.registerHmppsQueue(it, queueName, awsSqsDlqClient, dlqName) }
      }

  @Bean
  @ConditionalOnProperty(name = ["hmpps.sqs.provider"], havingValue = "aws")
  fun awsSqsDlqClient(sqsConfigProperties: SqsConfigProperties): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withCredentials(
        AWSStaticCredentialsProvider(
          BasicAWSCredentials(
            sqsConfigProperties.dlqAccessKeyId,
            sqsConfigProperties.dlqSecretAccessKey
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
