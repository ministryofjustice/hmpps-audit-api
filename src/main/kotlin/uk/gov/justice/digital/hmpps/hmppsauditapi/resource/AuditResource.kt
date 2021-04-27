package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class AuditResource(
  private val auditService: AuditService,
) {
  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @GetMapping("")
  fun findAll(): List<AuditDto> {
    return auditService.findAll()
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuditDto(
  val id: UUID,
  val what: String,
  val `when`: Instant,
  val operationId: String?,
  val who: String?,
  val service: String?,
  val details: String?
) {
  constructor(auditEvent: AuditEvent) : this(
    auditEvent.id!!, auditEvent.what, auditEvent.`when`, auditEvent.operationId, auditEvent.who,
    auditEvent.service, auditEvent.details
  )
}
