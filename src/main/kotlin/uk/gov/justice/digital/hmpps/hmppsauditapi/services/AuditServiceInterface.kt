package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditDto

interface AuditServiceInterface {
  fun saveAuditEvent(auditEvent: AuditEvent)

  fun findAll(): List<AuditDto>

  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto>

  fun triggerQuery(
    queryRequest: AuditQueryRequest,
    auditEventType: AuditEventType,
  ): AuditQueryResponse

  fun getQueryResults(queryExecutionId: String): AuditQueryResponse
}
