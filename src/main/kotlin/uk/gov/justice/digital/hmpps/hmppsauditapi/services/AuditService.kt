package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent

@Service
class AuditService(private val telemetryClient: TelemetryClient, private val auditRepository: AuditRepository) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun audit(auditEvent: AuditEvent) {
    log.info("About to audit $auditEvent")
    telemetryClient.trackEvent("hmpps-audit", auditEvent.asMap())
    auditRepository.save(auditEvent)
  }
}

private fun AuditEvent.asMap(): Map<String, String> {
  var items = mutableMapOf("what" to what, "when" to `when`.toString())
  items.addIfNotNull("operationId", operationId)
  items.addIfNotNull("who", who)
  items.addIfNotNull("service", service)
  items.addIfNotNull("details", details)
  return items.toMap()
}

fun MutableMap<String, String>.addIfNotNull(key: String, value: String?) {
  value?.let { this.put(key, value) }
}
