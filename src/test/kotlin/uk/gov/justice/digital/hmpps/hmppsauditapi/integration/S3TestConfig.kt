package uk.gov.justice.digital.hmpps.hmppsauditapi.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import java.net.URI

@TestConfiguration
class S3TestConfig {

    @Bean
    fun s3Client(hmppsSqsProperties: HmppsSqsProperties): S3Client {
        return S3Client.builder()
            .endpointOverride(URI.create(hmppsSqsProperties.localstackUrl))
            .region(Region.of(hmppsSqsProperties.region))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()
    }
}
