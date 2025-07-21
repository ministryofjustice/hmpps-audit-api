package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Component
public class AthenaPropertiesFactory(
  @Qualifier("staffAthenaProperties") private val staffAthenaProperties: AthenaProperties,
  @Qualifier("prisonerAthenaProperties") private val prisonerAthenaProperties: AthenaProperties,
) {

  private val propertiesMap: Map<AuditEventType, AthenaProperties> = mapOf(
    AuditEventType.STAFF to staffAthenaProperties,
    AuditEventType.PRISONER to prisonerAthenaProperties,
  )

  fun getProperties(type: AuditEventType): AthenaProperties = propertiesMap[type]
    ?: throw IllegalArgumentException("No properties found for type $type")
}
