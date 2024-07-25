package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class AWSConfig {

  @Value("\${aws.region}")
  private lateinit var region: String

  @Bean
  fun s3Client(): S3Client {
    return S3Client.builder()
      .region(Region.of(region))
      .credentialsProvider(DefaultCredentialsProvider.create())
      .build()
  }
}
