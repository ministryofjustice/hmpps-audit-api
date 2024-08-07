package uk.gov.justice.digital.hmpps.hmppsauditapi.integration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import java.net.URI

@TestConfiguration
class S3TestConfig {

  @Value("\${aws.s3.auditBucketName}")
  private lateinit var bucketName: String

  @Bean
  @Profile("!circleci")
  fun s3Client(hmppsSqsProperties: HmppsSqsProperties): S3Client {
    val s3Client = S3Client.builder()
      .endpointOverride(URI.create(hmppsSqsProperties.localstackUrl))
      .region(Region.of(hmppsSqsProperties.region))
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
      .forcePathStyle(true)
      .build()
    if (s3Client.listBuckets().buckets().size == 0) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
    }
    return s3Client
  }
}
