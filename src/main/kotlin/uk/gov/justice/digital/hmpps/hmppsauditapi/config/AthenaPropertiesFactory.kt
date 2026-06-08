package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Component
class AthenaPropertiesFactory(
  @Qualifier("staffAthenaProperties") private val staffAthenaProperties: AthenaProperties,
  @Qualifier("prisonerAthenaProperties") private val prisonerAthenaProperties: AthenaProperties,
  @Qualifier("personOnProbationAthenaProperties") private val personOnProbationAthenaProperties: AthenaProperties,
) {

  private val propertiesMap: Map<AuditEventType, AthenaProperties> = mapOf(
    AuditEventType.STAFF to staffAthenaProperties,
    AuditEventType.PRISONER to prisonerAthenaProperties,
    AuditEventType.PERSON_ON_PROBATION to personOnProbationAthenaProperties,
  )

  fun getProperties(type: AuditEventType): AthenaProperties = propertiesMap[type]
    ?: throw IllegalArgumentException("No properties found for type $type")
}
