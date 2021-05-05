package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SqsConfig {

  @Bean
  @Primary
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "aws")
  fun awsSqsClient(
    @Value("\${sqs.aws.access.key.id}") accessKey: String,
    @Value("\${sqs.aws.secret.access.key}") secretKey: String,
    @Value("\${sqs.endpoint.region}") region: String
  ): AmazonSQSAsync =
    AmazonSQSAsyncClientBuilder.standard()
      .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
      .withRegion(region)
      .build()

  @Bean
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "aws")
  fun awsSqsDlqClient(
    @Value("\${sqs.aws.dlq.access.key.id}") accessKey: String,
    @Value("\${sqs.aws.dlq.secret.access.key}") secretKey: String,
    @Value("\${sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
      .withRegion(region)
      .build()
}
