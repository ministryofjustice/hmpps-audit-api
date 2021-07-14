package uk.gov.justice.digital.hmpps.hmppsauditapi

import com.amazonaws.services.sqs.AmazonSQS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import uk.gov.justice.hmpps.sqs.HmppsQueueFactory
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTest.SqsConfig::class)
@ActiveProfiles("test")
abstract class IntegrationTest {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @SpyBean
  protected lateinit var auditService: AuditService

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAuthorisation(
    user: String = "hmpps-audit-client",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  @Autowired
  private lateinit var hmppsSqsProperties: HmppsSqsProperties

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  protected val auditQueueConfig by lazy {
    hmppsQueueService.findByQueueId("auditqueue") ?: throw MissingQueueException("HmppsQueue auditqueue not found")
  }

  class MissingQueueException(message: String) : RuntimeException(message)

  protected val awsSqsDlqClient: AmazonSQS by lazy { auditQueueConfig.sqsDlqClient }
  protected val awsSqsUrl: String by lazy { auditQueueConfig.queueUrl }
  protected val awsSqsDlqUrl: String by lazy { auditQueueConfig.dlqUrl }

  @SpyBean(name = "auditqueue-sqs-client")
  protected lateinit var awsSqsClient: AmazonSQS

  @TestConfiguration
  class SqsConfig(private val hmppsQueueFactory: HmppsQueueFactory) {

    @Bean("auditqueue-sqs-client")
    fun auditQueueSqsClient(
      hmppsSqsProperties: HmppsSqsProperties,
      @Qualifier("auditqueue-sqs-dlq-client") auditQueueSqsDlqClient: AmazonSQS
    ): AmazonSQS =
      with(hmppsSqsProperties) {
        val config = queues["auditqueue"]
          ?: throw uk.gov.justice.hmpps.sqs.MissingQueueException("HmppsSqsProperties config for auditqueue not found")
        hmppsQueueFactory.createSqsClient("auditqueue", config, hmppsSqsProperties, auditQueueSqsDlqClient)
      }
  }
}
