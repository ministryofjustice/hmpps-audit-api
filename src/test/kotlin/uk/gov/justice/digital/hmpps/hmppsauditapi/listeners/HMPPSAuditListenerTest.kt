package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest

internal class HMPPSAuditListenerTest : QueueListenerIntegrationTest() {

  @Test
  internal fun `will call service for an audit event with JSON details`() {
    val message = """
    {
      "what": "OFFENDER_DELETED",
      "when": "2021-01-25T12:30:00Z",
      "who": "bobby.beans",
      "service": "offender-service",
      "details": "{ \"offenderId\": \"99\" }"
    }
  """
    listener.onAuditEvent(message)

    doNothing().whenever(auditService).audit(any())

    verify(auditService).audit(
      check {
        assertThat(it.details).isEqualTo("{ \"offenderId\": \"99\" }")
        assertThat(it.`when`).isEqualTo("2021-01-25T12:30:00Z")
        assertThat(it.what).isEqualTo("OFFENDER_DELETED")
        assertThat(it.who).isEqualTo("bobby.beans")
        assertThat(it.service).isEqualTo("offender-service")
      },
    )
  }

  @Test
  internal fun `will call service for an audit event with regular string details`() {
    val message = """
    {
      "what": "USER_LOGIN",
      "when": "2021-01-25T12:35:00Z",
      "who": "alice.jones",
      "service": "auth-service",
      "details": "non-json-stringified details"
    }
  """

    doNothing().whenever(auditService).audit(any())

    listener.onAuditEvent(message)

    verify(auditService).audit(
      check {
        assertThat(it.details).isEqualTo("{\"details\":\"non-json-stringified details\"}")
        assertThat(it.`when`).isEqualTo("2021-01-25T12:35:00Z")
        assertThat(it.what).isEqualTo("USER_LOGIN")
        assertThat(it.who).isEqualTo("alice.jones")
        assertThat(it.service).isEqualTo("auth-service")
      },
    )
  }
}
