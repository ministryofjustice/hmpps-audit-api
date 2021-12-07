package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.time.Instant
import java.util.UUID

@Suppress("ClassName")
class AuditResourceTest : IntegrationTest() {

  @MockBean
  private lateinit var auditRepository: AuditRepository

  @TestInstance(PER_CLASS)
  @Nested
  inner class SecureGetEndpoints {
    private fun secureEndpoints() =
      listOf(
        "/audit",
        "/audit/paged"
      )

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but no scope`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but wrong scope`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role and scope`(uri: String) {
      whenever(auditRepository.findPage(any(), anyOrNull(), anyOrNull())).thenReturn(
        PageImpl(listOf())
      )

      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @TestInstance(PER_CLASS)
  @Nested
  inner class securePostEndpoints {
    private fun secureEndpoints() =
      listOf(
        "/audit",
      )

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(AuditEvent(what = "what")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but no scope`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(BodyInserters.fromValue(AuditEvent(what = "what")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but wrong scope`(uri: String) {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
        .body(BodyInserters.fromValue(AuditEvent(what = "what")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role and scope`(uri: String) {
      val auditEvent = AuditEvent(what = "secureEndpointCheck")

      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .body(BodyInserters.fromValue(auditEvent))
        .exchange()
        .expectStatus().isAccepted

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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

      verify(auditService).sendAuditEvent(eventWithOperationId)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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
            """.trimIndent()
          )
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
    }

    @Test
    internal fun `details is json`() {
      val auditEvent = AuditEvent(what = "json details", `when` = Instant.parse("2021-03-01T15:15:30Z"), details = "{\"offenderId\": \"97\"}")

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
            """.trimIndent()
          )
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
    }

    @Test
    internal fun `details is a string`() {
      val auditEvent = AuditEvent(what = "string details", details = "{\"details\":\"a test\"}", `when` = Instant.parse("2021-04-01T15:15:30Z"))

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
            """.trimIndent()
          )
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
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
            """.trimIndent()
          )
        )
        .exchange()
        .expectStatus().isAccepted

      verify(auditService).sendAuditEvent(auditEvent)
      verify(awsSqsClient).sendMessage(eq(auditQueueConfig.queueUrl), anyString())
    }
  }

  @Nested
  inner class findAuditEntries {
    @Test
    fun `find all audit entries`() {
      val listOfAudits = listOf(
        AuditEvent(
          UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
          "MINIMUM_FIELDS_EVENT",
          Instant.parse("2021-04-04T17:17:30Z")
        ),
        AuditEvent(
          UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
          "COURT_REGISTER_BUILDING_UPDATE",
          Instant.parse("2021-04-03T10:15:30Z"),
          "badea6d876c62e2f5264c94c7b50875e",
          "bobby.beans",
          "court-register",
          "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
        ),
        AuditEvent(
          UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
          "OFFENDER_DELETED",
          Instant.parse("2021-04-01T15:15:30Z"),
          "cadea6d876c62e2f5264c94c7b50875e",
          "bobby.beans",
          "offender-service",
          "{\"offenderId\": \"97\"}"
        ),
        AuditEvent(
          UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
          "OFFENDER_DELETED",
          Instant.parse("2020-12-31T08:11:30Z"),
          "dadea6d876c62e2f5264c94c7b50875e",
          "freddy.frog",
          "offender-service",
          "{\"offenderId\": \"98\"}"
        )
      )
      whenever(auditRepository.findAll(Sort.by(DESC, "when"))).thenReturn(
        listOfAudits
      )

      webTestClient.get().uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("audit_events".loadJson())

      verify(auditRepository).findAll(Sort.by(DESC, "when"))
    }

    private fun String.loadJson(): String {
      return AuditResourceTest::class.java.getResource("$this.json").readText()
    }
  }
}
