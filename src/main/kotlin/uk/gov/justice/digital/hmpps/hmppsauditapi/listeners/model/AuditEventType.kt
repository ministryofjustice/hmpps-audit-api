package uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model

enum class AuditEventType(val description: String) {
  STAFF("hmpps-audit"),
  PRISONER("hmpps-prisoner-audit"),
}
