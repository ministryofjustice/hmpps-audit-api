package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

class AuditConsolidationConfigTest {
  private val propertiesFactory = mock<AthenaPropertiesFactory>()

  @Test
  fun `enabled consolidations require each enabled type to have an audit bucket`() {
    whenever(propertiesFactory.getProperties(AuditEventType.STAFF)).thenReturn(properties(AuditEventType.STAFF, "staff-audit"))
    whenever(propertiesFactory.getProperties(AuditEventType.PERSON_ON_PROBATION))
      .thenReturn(properties(AuditEventType.PERSON_ON_PROBATION, ""))

    val error = assertThrows<IllegalArgumentException> {
      AuditConsolidationConfig(true, setOf(AuditEventType.STAFF, AuditEventType.PERSON_ON_PROBATION), propertiesFactory)
    }

    assertThat(error.message).contains("PERSON_ON_PROBATION")
  }

  @Test
  fun `enabled types with audit buckets are accepted`() {
    whenever(propertiesFactory.getProperties(AuditEventType.STAFF)).thenReturn(properties(AuditEventType.STAFF, "staff-audit"))
    whenever(propertiesFactory.getProperties(AuditEventType.PRISONER)).thenReturn(properties(AuditEventType.PRISONER, "prisoner-audit"))

    val config = AuditConsolidationConfig(true, setOf(AuditEventType.STAFF, AuditEventType.PRISONER), propertiesFactory)

    assertThat(config.enabledAuditEventTypes).containsExactlyInAnyOrder(AuditEventType.STAFF, AuditEventType.PRISONER)
  }

  @Test
  fun `disabled consolidations do not check audit buckets`() {
    AuditConsolidationConfig(false, AuditEventType.entries.toSet(), propertiesFactory)

    verifyNoInteractions(propertiesFactory)
  }

  private fun properties(type: AuditEventType, bucketName: String) = AthenaProperties(type, bucketName, "", "", "", "")
}
