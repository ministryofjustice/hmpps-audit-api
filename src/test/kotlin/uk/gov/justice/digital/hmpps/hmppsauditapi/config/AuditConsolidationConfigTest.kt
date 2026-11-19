package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

class AuditConsolidationConfigTest {
  private val propertiesFactory = mock<AthenaPropertiesFactory>()

  @Test
  fun `enabled consolidations require a bucket`() {
    assertThrows<IllegalArgumentException> {
      AuditConsolidationConfig(true, "", setOf(AuditEventType.STAFF), propertiesFactory)
    }
  }

  @Test
  fun `consolidation bucket cannot be an existing ingestion bucket`() {
    AuditEventType.entries.forEach { type ->
      whenever(propertiesFactory.getProperties(type)).thenReturn(
        AthenaProperties(type, "${type.name.lowercase()}-audit", "", "", "", ""),
      )
    }
    AuditEventType.entries.forEach { type ->
      val error = assertThrows<IllegalArgumentException> {
        AuditConsolidationConfig(true, "${type.name.lowercase()}-audit", setOf(AuditEventType.STAFF), propertiesFactory)
      }
      assertThat(error.message).contains("separate bucket")
    }
  }

  @Test
  fun `separate consolidation bucket is accepted`() {
    AuditEventType.entries.forEach { type ->
      whenever(propertiesFactory.getProperties(type)).thenReturn(
        AthenaProperties(type, "${type.name.lowercase()}-audit", "", "", "", ""),
      )
    }
    val config = AuditConsolidationConfig(true, "consolidations", setOf(AuditEventType.STAFF), propertiesFactory)
    assertThat(config.bucketName).isEqualTo("consolidations")
  }
}
