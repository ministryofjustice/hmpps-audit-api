package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@ActiveProfiles("test")
@SpringBootTest
@ContextConfiguration(classes = [AthenaPropertiesConfig::class, AthenaPropertiesFactory::class])
class AthenaPropertiesFactoryIntegrationTest {

  @Autowired
  private lateinit var athenaPropertiesFactory: AthenaPropertiesFactory

  @Test
  fun `should return correct staff credentials`() {
    val credentials = athenaPropertiesFactory.getProperties(AuditEventType.STAFF)

    assertThat(credentials.databaseName).isEqualTo("the-database")
    assertThat(credentials.tableName).isEqualTo("the-table")
    assertThat(credentials.workGroupName).isEqualTo("the-workgroup")
    assertThat(credentials.outputLocation).isEqualTo("the-location")
  }

  @Test
  fun `should return correct prison credentials`() {
    val credentials = athenaPropertiesFactory.getProperties(AuditEventType.PRISONER)

    assertThat(credentials.databaseName).isEqualTo("the-prisoner-database")
    assertThat(credentials.tableName).isEqualTo("the-prisoner-table")
    assertThat(credentials.workGroupName).isEqualTo("the-prisoner-workgroup")
    assertThat(credentials.outputLocation).isEqualTo("the-prisoner-location")
  }
}
