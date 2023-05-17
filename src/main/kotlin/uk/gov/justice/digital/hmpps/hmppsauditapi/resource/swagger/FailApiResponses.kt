package uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.ErrorResponse
import java.lang.annotation.Inherited

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Forbidden. Requires authorisation with correct role.",
      content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))],
    ),
  ],
)
annotation class FailApiResponses
