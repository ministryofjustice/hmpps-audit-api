package uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@FailApiResponses
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "204",
      description = "Deleted",
      content = [Content(mediaType = "application/json")],
    ),
  ],
)
annotation class DeleteApiResponses
