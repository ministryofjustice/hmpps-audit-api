package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.time.Instant
import java.util.UUID

// This is a hack to get around the fact that springdocs responses cannot contain generics
class AuditDtoPage : PageImpl<AuditDto>(mutableListOf<AuditDto>())

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
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AuditDto::class)))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
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

  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @GetMapping("/paged")
  @Operation(
    summary = "Get page of audit events",
    description = "Page of audit events",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Paged Audit Events Returned",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = AuditDtoPage::class)))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_AUDIT",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
    @RequestParam who: String? = null,
    @RequestParam what: String? = null,
  ): Page<AuditDto> = auditService.findPage(pageable, who, what)

  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Add a new audit event",
    description = "Adds a new Audit Event to the audit queue, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json")]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Audit Event Added to audit event queue",
        content = [Content(mediaType = "application/json")]
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request to add an audit event",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_AUDIT",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  fun insertAuditEvent(@RequestBody auditEvent: AuditEvent) {
    auditService.sendAuditEvent(auditEvent)
  }
}

@JsonInclude(NON_NULL)
@Schema(description = "Audit Event Information")
data class AuditDto(
  @Schema(description = "Audit Event Id", example = "0f21b9e0-d153-42c6-a9ab-d583fe590987")
  val id: UUID,
  @Schema(description = "Detailed description of the Event", example = "COURT_REGISTER_BUILDING_UPDATE")
  val what: String,
  @Schema(description = "When the Event occurred", example = "2021-04-01T15:15:30Z")
  val `when`: Instant,
  @Schema(description = "The App Insights operation Id for the Event", example = "cadea6d876c62e2f5264c94c7b50875e")
  val operationId: String?,
  @Schema(description = "Who initiated the Event", example = "fred.smith@myemail.com")
  val who: String?,
  @Schema(description = "Which service the Event relates to", example = "court-register")
  val service: String?,
  @Schema(description = "Additional information", example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}")
  val details: String?
) {
  constructor(auditEvent: AuditEvent) : this(
    auditEvent.id!!, auditEvent.what, auditEvent.`when`, auditEvent.operationId, auditEvent.who,
    auditEvent.service, auditEvent.details
  )
}
