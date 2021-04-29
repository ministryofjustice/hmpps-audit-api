package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.ErrorResponse
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
  @Operation(
    summary = "Get all audit events",
    description = "Get all audit events, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "All Audit Events Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = AuditDto::class))
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid Oauth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_AUDIT",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
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
