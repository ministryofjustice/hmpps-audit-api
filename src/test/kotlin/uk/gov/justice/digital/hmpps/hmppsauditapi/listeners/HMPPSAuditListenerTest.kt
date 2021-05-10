package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService

internal class HMPPSAuditListenerTest {
  private val auditService: AuditService = mock()
  private val listener: HMPPSAuditListener =
    HMPPSAuditListener(auditService = auditService, ObjectMapper().registerModule(JavaTimeModule()))

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
