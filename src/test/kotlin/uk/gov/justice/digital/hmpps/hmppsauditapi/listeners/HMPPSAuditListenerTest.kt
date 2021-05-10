package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService

internal class HMPPSAuditListenerTest : QueueListenerIntegrationTest() {
  @MockBean
  private lateinit var auditService: AuditService

  @Autowired
  private lateinit var listener: HMPPSAuditListener

  @Test
  internal fun `will call service for an audit event`() {
    val message = """
    {
      "what": "OFFENDER_DELETED",
      "when": "2021-01-25T12:30:00Z",
      "who": "bobby.beans",
      "service": "offender-service",
      "details": "{ \"offenderId\": \"99\"}"
    }
  """
    listener.onAuditEvent(message)

    verify(auditService).audit(
      check {
        assertThat(it.details).isEqualTo("{ \"offenderId\": \"99\"}")
        assertThat(it.`when`).isEqualTo("2021-01-25T12:30:00Z")
        assertThat(it.what).isEqualTo("OFFENDER_DELETED")
        assertThat(it.who).isEqualTo("bobby.beans")
        assertThat(it.service).isEqualTo("offender-service")
      }
    )
  }
}
