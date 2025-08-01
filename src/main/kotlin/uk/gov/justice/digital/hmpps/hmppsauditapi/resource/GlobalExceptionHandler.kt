package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent

@ControllerAdvice
class GlobalExceptionHandler(private val telemetryClient: TelemetryClient) : ResponseEntityExceptionHandler() {

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleDeserializationError(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, String>> {
    ex.printStackTrace()
    telemetryClient.trackEvent("Deserialization Error", mapOf("message" to ex.message.toString()))
    return ResponseEntity.badRequest().body(
      mapOf(
        "error" to "Malformed JSON or incorrect field types",
        "message" to (ex.mostSpecificCause.message ?: ex.message ?: "Unknown error"),
      ),
    )
  }

  @ExceptionHandler(Exception::class)
  fun handleAll(ex: Exception): ResponseEntity<Map<String, String>> {
    ex.printStackTrace()
    telemetryClient.trackEvent("Unexpected Server Error", mapOf("message" to ex.message.toString()))
    return ResponseEntity.internalServerError().body(
      mapOf(
        "error" to "Unexpected server error",
        "message" to (ex.message ?: "Unknown error"),
      ),
    )
  }
}
