package uk.gov.justice.digital.hmpps.hmppsauditapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditServiceFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.PrisonerAuditService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.StaffAuditService

class AuditServiceFactoryTest {

  private val prisonerAuditService: PrisonerAuditService = mock()
  private val staffAuditService: StaffAuditService = mock()

  private val auditServiceFactory = AuditServiceFactory(prisonerAuditService, staffAuditService)

  @Test
  fun `should return PrisonerAuditService for PRISONER event type`() {
    val service = auditServiceFactory.getAuditService(AuditEventType.PRISONER)
    assertEquals(prisonerAuditService, service)
  }

  @Test
  fun `should return StaffAuditService for STAFF event type`() {
    val service = auditServiceFactory.getAuditService(AuditEventType.STAFF)
    assertEquals(staffAuditService, service)
  }
}
