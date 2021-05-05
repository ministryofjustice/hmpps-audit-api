package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LocalStackConfig {

  @Bean("awsSqsClient")
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "localstack")
  fun awsSqsClientLocalStack(
    @Value("\${sqs.endpoint.url}") serviceEndpoint: String,
    @Value("\${sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  @Bean("awsSqsDlqClient")
  @ConditionalOnProperty(name = ["sqs.provider"], havingValue = "localstack")
  fun awsSqsDlqClientLocalStack(
    @Value("\${sqs.endpoint.url}") serviceEndpoint: String,
    @Value("\${sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()
}