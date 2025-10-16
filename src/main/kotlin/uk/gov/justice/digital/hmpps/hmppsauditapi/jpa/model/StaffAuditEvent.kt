package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.time.Instant
import java.util.UUID

@Entity(name = "StaffAuditEvent")
@Table(name = "AuditEvent", schema = "staff")
@Schema(description = "Audit Event Insert Record")
data class StaffAuditEvent(
  @Id
  @GeneratedValue
  @Hidden
  var id: UUID? = null,
  val what: String,
  @Column(name = "occurred")
  val `when`: Instant = Instant.now(),
  val operationId: String? = null,
  val subjectId: String? = null,
  val subjectType: String = "NOT_APPLICABLE",
  val correlationId: String? = null,
  val who: String? = null,
  val service: String? = null,
  val details: String? = null,
)

fun StaffAuditEvent.toAuditEvent(): AuditEvent = AuditEvent(
  id = id,
  what = what,
  `when` = `when`,
  operationId = operationId,
  subjectId = subjectId,
  subjectType = subjectType,
  correlationId = correlationId,
  who = who,
  service = service,
  details = details,
)
