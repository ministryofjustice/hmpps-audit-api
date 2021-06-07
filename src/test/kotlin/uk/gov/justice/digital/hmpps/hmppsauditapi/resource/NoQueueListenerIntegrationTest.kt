package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.test.context.ActiveProfiles

abstract class NoQueueListenerIntegrationTest : IntegrationTest() {
  @MockBean
  protected lateinit var queueMessagingTemplate: QueueMessagingTemplate
}
