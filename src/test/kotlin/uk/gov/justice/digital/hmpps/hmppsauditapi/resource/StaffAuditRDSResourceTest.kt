package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.time.Instant

@Suppress("ClassName")
class StaffAuditRDSResourceTest : IntegrationTest() {

  @TestInstance(PER_CLASS)
  @Nested
  inner class SecureGetEndpoints {
    private fun secureEndpointsGet() = listOf(
      "/audit",
    )

    @ParameterizedTest
    @MethodSource("secureEndpointsGet")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @TestInstance(PER_CLASS)
  @Nested
  inner class securePostEndpoints {
    private fun secureEndpointsPost() = listOf(
      "/audit",
    )

    @ParameterizedTest
    @MethodSource("secureEndpointsPost")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsPost")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(AuditEvent(what = "what")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsPost")
    internal fun `satisfies the correct role but no scope`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(BodyInserters.fromValue(AuditEvent(what = "what")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsPost")
    internal fun `satisfies the correct role but wrong scope`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
        .body(BodyInserters.fromValue(AuditEvent(what = "what")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsPost")
    internal fun `satisfies the correct role and scope`(uri: String) {
      val auditEvent = AuditEvent(what = "secureEndpointCheck")

      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }
  }

  @Nested
  inner class traceParentHeader {
    @Test
    internal fun `no traceparent header`() {
      val auditEvent = AuditEvent(what = "traceparent empty event")

      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }

    @Test
    internal fun `invalid traceparent header`() {
      val auditEvent = AuditEvent(what = "traceparent invalid event")

      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .header("traceparent", "3d6cb11f59448eb9d50a7f1e5237")
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }

    @Test
    internal fun `valid traceparent header and operationId`() {
      val auditEvent = AuditEvent(what = "traceparent valid event", operationId = "1234cb11f59448eb9d50a7f1e523748")

      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .header("traceparent", "00-8c3d6cb11f59448eb9d50a7f1e523748-adf0569620934d76-01")
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }

    @Test
    internal fun `valid traceparent header and no operation id`() {
      val auditEvent = AuditEvent(what = "traceparent valid event")
      val eventWithOperationId = auditEvent.copy(operationId = "8c3d6cb11f59448eb9d50a7f1e523748")

      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .header("traceparent", "00-8c3d6cb11f59448eb9d50a7f1e523748-adf0569620934d76-01")
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(eventWithOperationId)
      // test call to queue
    }

    @Test
    internal fun `operationId not overwritten by traceparent header`() {
      val auditEvent = AuditEvent(what = "traceparent", operationId = "123456789")

      webTestClient.post()
        .uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .header("traceparent", "00-8c3d6cb11f59448eb9d50a7f1e523748-adf0569620934d76-01")
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }
  }

  @Nested
  inner class detailsStoredAsJson {
    @Test
    internal fun `details is null`() {
      val auditEvent = AuditEvent(what = "null details", `when` = Instant.parse("2021-02-01T15:15:30Z"))

      webTestClient.post()
        .uri("/audit")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            """
              {
                "what": "null details",
                "when": "2021-02-01T15:15:30Z"
              }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }

    @Test
    internal fun `details is json`() {
      val auditEvent = AuditEvent(
        what = "json details",
        `when` = Instant.parse("2021-03-01T15:15:30Z"),
        details = "{\"offenderId\": \"97\"}",
      )

      webTestClient.post()
        .uri("/audit")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            """
              {
                "what": "json details",
                "when": "2021-03-01T15:15:30Z",
                "details": "{\"offenderId\": \"97\"}"
              }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }

    @Test
    internal fun `details is a string`() {
      val auditEvent = AuditEvent(
        what = "string details",
        details = "{\"details\":\"a test\"}",
        `when` = Instant.parse("2021-04-01T15:15:30Z"),
      )

      webTestClient.post()
        .uri("/audit")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            """
              {
                "what": "string details",
                "when": "2021-04-01T15:15:30Z",
                "details": "a test"
              }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }

    @Test
    internal fun `details is an empty(blank) string`() {
      val auditEvent = AuditEvent(what = "string details", `when` = Instant.parse("2021-05-01T15:15:30Z"))

      webTestClient.post()
        .uri("/audit")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(
          BodyInserters.fromValue(
            """
              {
                "what": "string details",
                "when": "2021-05-01T15:15:30Z",
                "details": "   "
              }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditQueueService).sendAuditEvent(auditEvent)
      // test call to queue
    }
  }
}
