package uk.gov.justice.digital.hmpps.hmppsauditapi

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
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsauditapi.integration.S3TestConfig
import uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend.CommandLineProfilesResolver
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import uk.gov.justice.hmpps.sqs.HmppsQueueFactory
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTest.SqsConfig::class, JwtAuthHelper::class, S3TestConfig::class)
@ActiveProfiles(resolver = CommandLineProfilesResolver::class)
abstract class IntegrationTest {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @SpyBean
  protected lateinit var auditService: AuditService

  @SpyBean
  protected lateinit var auditQueueService: AuditQueueService

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAuthorisation(
    user: String = "hmpps-audit-client",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  @Autowired
  private lateinit var hmppsSqsProperties: HmppsSqsProperties

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  protected val auditQueueConfig by lazy {
    hmppsQueueService.findByQueueId("auditqueue") ?: throw MissingQueueException("HmppsQueue auditqueue not found")
  }

  protected val prisonerAuditQueueConfig by lazy {
    hmppsQueueService.findByQueueId("prisonerauditqueue") ?: throw MissingQueueException("HmppsQueue prisonerauditqueue not found")
  }

  protected val auditUsersQueueConfig by lazy {
    hmppsQueueService.findByQueueId("auditusersqueue") ?: throw MissingQueueException("HmppsQueue auditusersqueue not found")
  }

  protected val awsSqsDlqClient by lazy { auditQueueConfig.sqsDlqClient as SqsAsyncClient }
  protected val awsSqsUrl by lazy { auditQueueConfig.queueUrl }
  protected val awsSqsDlqUrl by lazy { auditQueueConfig.dlqUrl as String }

  protected val awsSqsPrisonerAuditDlqClient by lazy { prisonerAuditQueueConfig.sqsDlqClient as SqsAsyncClient }
  protected val awsSqsPrisonerAuditDlqUrl by lazy { prisonerAuditQueueConfig.dlqUrl as String }

  protected val staffAthenaProperties = AthenaProperties(
    auditEventType = AuditEventType.STAFF,
    s3BucketName = "hmpps-audit-bucket",
    databaseName = "the-database",
    tableName = "the-table",
    workGroupName = "the-workgroup",
    outputLocation = "the-location",
  )

  protected val prisonerAthenaProperties = AthenaProperties(
    auditEventType = AuditEventType.PRISONER,
    s3BucketName = "hmpps-prisoner-audit-bucket",
    databaseName = "the-prisoner-database",
    tableName = "the-prisoner-table",
    workGroupName = "the-prisoner-workgroup",
    outputLocation = "the-prisoner-location",
  )

  @SpyBean
  @Qualifier("auditqueue-sqs-client")
  protected lateinit var awsSqsClient: SqsAsyncClient

  @TestConfiguration
  class SqsConfig(private val hmppsQueueFactory: HmppsQueueFactory) {
    @Bean("auditqueue-sqs-client")
    fun outboundQueueSqsClient(
      hmppsSqsProperties: HmppsSqsProperties,
      @Qualifier("auditqueue-sqs-dlq-client") outboundQueueSqsDlqClient: SqsAsyncClient,
    ): SqsAsyncClient = with(hmppsSqsProperties) {
      val config = queues["auditqueue"]
        ?: throw MissingQueueException("HmppsSqsProperties config for auditqueue not found")
      hmppsQueueFactory.createSqsAsyncClient(config, hmppsSqsProperties, outboundQueueSqsDlqClient)
    }

    @Bean("prisonerauditqueue-sqs-client")
    fun outboundPrisonerAuditQueueSqsClient(
      hmppsSqsProperties: HmppsSqsProperties,
      @Qualifier("prisonerauditqueue-sqs-dlq-client") outboundPrisonerAuditQueueSqsDlqClient: SqsAsyncClient,
    ): SqsAsyncClient = with(hmppsSqsProperties) {
      val config = queues["prisonerauditqueue"]
        ?: throw MissingQueueException("HmppsSqsProperties config for prisonerauditqueue not found")
      hmppsQueueFactory.createSqsAsyncClient(config, hmppsSqsProperties, outboundPrisonerAuditQueueSqsDlqClient)
    }

    @Bean("auditusersqueue-sqs-client")
    fun outboundAuditUsersQueueSqsClient(
      hmppsSqsProperties: HmppsSqsProperties,
      @Qualifier("auditusersqueue-sqs-dlq-client") outboundAuditUsersQueueSqsClient: SqsAsyncClient,
    ): SqsAsyncClient = with(hmppsSqsProperties) {
      val config = queues["auditusersqueue"]
        ?: throw MissingQueueException("HmppsSqsProperties config for auditusersqueue not found")
      hmppsQueueFactory.createSqsAsyncClient(config, hmppsSqsProperties, outboundAuditUsersQueueSqsClient)
    }
  }
}
