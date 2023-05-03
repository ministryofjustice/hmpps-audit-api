package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditService
import java.io.IOException
import java.time.Instant
import java.util.UUID

// This is a hack to get around the fact that springdocs responses cannot contain generics
class AuditDtoPage : PageImpl<AuditDto>(mutableListOf<AuditDto>())

@RestController
@RequestMapping("/audit", produces = [MediaType.APPLICATION_JSON_VALUE])
class AuditResource(
  private val auditService: AuditService,
  private val auditQueueService: AuditQueueService,
) {
  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_read')")
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
            array = ArraySchema(schema = Schema(implementation = AuditDto::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_AUDIT",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun findAll(): List<AuditDto> {
    return auditService.findAll()
  }

  @PreAuthorize("hasRole('ROLE_AUDIT')")
  @PostMapping("/paged")
  @Operation(
    summary = "Get paged audit events",
    description = "Get pages audit events, role required is ROLE_AUDIT",
    security = [SecurityRequirement(name = "ROLE_AUDIT")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Filtered Audit Events Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = AuditDto::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request, search criteria must be valid when supplied",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_AUDIT",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
    @RequestBody @Valid
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto> {
    return auditService.findPage(pageable, auditFilterDto)
  }

  @Deprecated("Audit events should be sent via audit queue")
  @PreAuthorize("hasRole('ROLE_AUDIT') and hasAuthority('SCOPE_write')")
  @PostMapping("")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(hidden = true)
  fun insertAuditEvent(
    @RequestHeader(value = "traceparent", required = false) traceParent: String?,
    @RequestBody auditEvent: AuditEvent,
  ) {
    val cleansedAuditEvent = auditEvent.copy(
      operationId = auditEvent.operationId ?: traceParent?.traceId(),
      details = auditEvent.details?.jsonString(),
    )
    auditQueueService.sendAuditEvent(cleansedAuditEvent)
  }

  private fun String.traceId(): String? {
    val traceParentElements = split("-")
    return if (traceParentElements.size == 4) traceParentElements[1] else null
  }

  private fun String.jsonString(): String? {
    return try {
      jacksonObjectMapper().readTree(trim())
      ifBlank { null }
    } catch (e: IOException) {
      "{\"details\":\"$this\"}"
    }
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
  @Schema(
    description = "Additional information",
    example = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}",
  )
  val details: String?,
) {
  constructor(auditEvent: AuditEvent) : this(
    auditEvent.id!!,
    auditEvent.what,
    auditEvent.`when`,
    auditEvent.operationId,
    auditEvent.who,
    auditEvent.service,
    auditEvent.details,
  )
}
