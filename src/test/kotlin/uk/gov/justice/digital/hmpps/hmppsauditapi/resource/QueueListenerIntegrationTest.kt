package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditS3Client
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

abstract class QueueListenerIntegrationTest : IntegrationTest() {

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @MockBean
  lateinit var auditRepository: AuditRepository

  @MockBean
  lateinit var auditS3Client: AuditS3Client

  @Autowired
  protected lateinit var listener: HMPPSAuditListener

  internal val auditQueue by lazy { hmppsQueueService.findByQueueId("auditqueue") as HmppsQueue }
  internal val auditQueueSqsClient by lazy { auditQueue.sqsClient }
  internal val auditQueueUrl by lazy { auditQueue.queueUrl }
  internal val auditSqsDlqClient by lazy { auditQueue.sqsDlqClient as SqsAsyncClient }
  internal val auditDlqUrl by lazy { auditQueue.dlqUrl as String }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int = auditQueueSqsClient.countAllMessagesOnQueue(auditQueueUrl).get()

  fun getNumberOfMessagesCurrentlyOnDlq(): Int = auditSqsDlqClient.countAllMessagesOnQueue(auditDlqUrl).get()
}
