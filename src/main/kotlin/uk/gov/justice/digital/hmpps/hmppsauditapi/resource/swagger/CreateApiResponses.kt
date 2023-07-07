package uk.gov.justice.digital.hmpps.hmppsauditapi.resource.swagger

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@FailApiResponses
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "201",
      description = "Created",
    ),
  ],
)
annotation class CreateApiResponses
