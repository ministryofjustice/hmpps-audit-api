package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditConsolidationService
import java.time.LocalDate

@RestController
@RequestMapping("/internal/integration-test/{auditEventType}/consolidations")
@PreAuthorize("hasRole('ROLE_AUDIT_INTEGRATION_TEST')")
class AuditConsolidationController(private val service: AuditConsolidationService) {
  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun submit(
    @PathVariable auditEventType: AuditEventType,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
  ) {
    service.startConsolidationJob(auditEventType, date)
  }
}
