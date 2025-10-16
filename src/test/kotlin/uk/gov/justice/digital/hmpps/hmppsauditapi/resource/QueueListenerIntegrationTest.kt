package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditS3Client
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @MockitoBean
  lateinit var telemetryClient: TelemetryClient

  @MockitoBean
  lateinit var staffAuditRepository: StaffAuditRepository

  @MockitoBean
  lateinit var prisonerAuditRepository: PrisonerAuditRepository

  @MockitoBean
  lateinit var auditS3Client: AuditS3Client

  @Autowired
  protected lateinit var listener: HMPPSAuditListener

  internal val auditQueue by lazy { hmppsQueueService.findByQueueId("auditqueue") as HmppsQueue }
  internal val auditQueueSqsClient by lazy { auditQueue.sqsClient }
  internal val auditQueueUrl by lazy { auditQueue.queueUrl }
  internal val auditSqsDlqClient by lazy { auditQueue.sqsDlqClient as SqsAsyncClient }
  internal val auditDlqUrl by lazy { auditQueue.dlqUrl as String }

  internal val prisonerAuditQueue by lazy { hmppsQueueService.findByQueueId("prisonerauditqueue") as HmppsQueue }
  internal val prisonerAuditQueueSqsClient by lazy { prisonerAuditQueue.sqsClient }
  internal val prisonerAuditQueueUrl by lazy { prisonerAuditQueue.queueUrl }
  internal val prisonerAuditSqsDlqClient by lazy { prisonerAuditQueue.sqsDlqClient as SqsAsyncClient }
  internal val prisonerAuditDlqUrl by lazy { prisonerAuditQueue.dlqUrl as String }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int = auditQueueSqsClient.countAllMessagesOnQueue(auditQueueUrl).get()
  fun getNumberOfMessagesCurrentlyOnPrisonerAuditQueue(): Int = prisonerAuditQueueSqsClient.countAllMessagesOnQueue(prisonerAuditQueueUrl).get()

  fun getNumberOfMessagesCurrentlyOnDlq(): Int = auditSqsDlqClient.countAllMessagesOnQueue(auditDlqUrl).get()
  fun getNumberOfMessagesCurrentlyOnPrisonerAuditDlq(): Int = prisonerAuditSqsDlqClient.countAllMessagesOnQueue(prisonerAuditDlqUrl).get()
}
