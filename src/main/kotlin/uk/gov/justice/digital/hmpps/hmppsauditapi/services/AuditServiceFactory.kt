package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Component
class AuditServiceFactory(
  private val prisonerAuditService: PrisonerAuditService,
  private val staffAuditService: StaffAuditService,
  private val personOnProbationAuditService: PersonOnProbationAuditService,
) {
  fun getAuditService(auditEventType: AuditEventType): AuditServiceInterface = when (auditEventType) {
    AuditEventType.PRISONER -> prisonerAuditService
    AuditEventType.STAFF -> staffAuditService
    AuditEventType.PERSON_ON_PROBATION -> personOnProbationAuditService
  }
}
