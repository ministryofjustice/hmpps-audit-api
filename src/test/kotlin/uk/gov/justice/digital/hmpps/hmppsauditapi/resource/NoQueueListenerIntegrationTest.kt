package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["no-queue-listener"])
abstract class NoQueueListenerIntegrationTest : IntegrationTest()
