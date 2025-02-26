package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.ColumnInfo
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState.SUCCEEDED
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.ResultSetMetadata
import software.amazon.awssdk.services.athena.model.Row
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Suppress("ClassName")
class AuditResourceTest : IntegrationTest() {

  @MockBean
  private lateinit var auditRepository: AuditRepository

  @Autowired
  private lateinit var athenaClient: AthenaClient

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

    @ParameterizedTest
    @MethodSource("secureEndpointsGet")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsGet")
    internal fun `satisfies the correct role but no scope`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsGet")
    internal fun `satisfies the correct role but wrong scope`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpointsGet")
    internal fun `satisfies the correct role and scope`(uri: String) {
      whenever(
        auditRepository.findPage(
          any(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(
        PageImpl(listOf()),
      )

      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
        .exchange()
        .expectStatus().isOk

      verify(auditQueueService).sendAuditAuditEvent(
        "AUDIT_GET_ALL",
        "",
      )

      verify(auditQueueService).sendAuditEvent(
        check {
          assertThat(it.what).isEqualTo("AUDIT_GET_ALL")
          assertThat(it.who).isEqualTo("hmpps-audit-client")
          assertThat(it.service).isEqualTo("hmpps-audit-api")
          assertThat(it.details).isEqualTo("\"\"")
        },
      )
      // test call to queue
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

  @Nested
  inner class findAuditEntries {
    @Test
    fun `find all audit entries`() {
      val listOfAudits = listOf(
        AuditEvent(
          UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
          "MINIMUM_FIELDS_EVENT",
          Instant.parse("2021-04-04T17:17:30Z"),
        ),
        AuditEvent(
          UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
          "COURT_REGISTER_BUILDING_UPDATE",
          Instant.parse("2021-04-03T10:15:30Z"),
          "badea6d876c62e2f5264c94c7b50875e",
          "er4ea6d876c62e2f5264c94c7b50863r",
          "PERSON",
          "er4ea6d876c62e2f5264c94c7b50863r",
          "bobby.beans",
          "court-register",
          "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
        ),
        AuditEvent(
          UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
          "OFFENDER_DELETED",
          Instant.parse("2021-04-01T15:15:30Z"),
          "cadea6d876c62e2f5264c94c7b50875e",
          "er4ea6d876c62e2f5264c94c7b50863r",
          "PERSON",
          "er4ea6d876c62e2f5264c94c7b50863r",
          "bobby.beans",
          "offender-service",
          "{\"offenderId\": \"97\"}",
        ),
        AuditEvent(
          UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
          "OFFENDER_DELETED",
          Instant.parse("2020-12-31T08:11:30Z"),
          "dadea6d876c62e2f5264c94c7b50875e",
          "er4ea6d876c62e2f5264c94c7b50863r",
          "PERSON",
          "er4ea6d876c62e2f5264c94c7b50863r",
          "freddy.frog",
          "offender-service",
          "{\"offenderId\": \"98\"}",
        ),
      )
      whenever(auditRepository.findAll(Sort.by(DESC, "when"))).thenReturn(
        listOfAudits,
      )

      webTestClient.get().uri("/audit")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("audit_events".loadJson())

      verify(auditRepository).findAll(Sort.by(DESC, "when"))
    }

    @Nested
    inner class findAuditEntriesForStaffMember {
      @Test
      fun `find all audit entries`() {
        val expectedAuditDto = AuditDto(
          id = UUID.randomUUID(),
          what = "READ_USER",
          `when` = Instant.parse("2024-02-14T12:34:56Z"),
          operationId = "op-123",
          subjectId = "sub-456",
          subjectType = "User",
          correlationId = "corr-789",
          who = "test-user",
          service = "auth-service",
          details = "some details",
        )

        val startQueryExecutionRequest: StartQueryExecutionRequest = StartQueryExecutionRequest.builder()
          .queryString("SELECT * FROM the-database.audit_event WHERE DATE(`when`) BETWEEN DATE '2025-01-01' AND DATE '2025-01-31' AND subjectId = 'test-subject' AND subjectType = 'USER_ID';")
          .queryExecutionContext(QueryExecutionContext.builder().database("the-database").build())
          .workGroup("the-workgroup")
          .resultConfiguration(ResultConfiguration.builder().outputLocation("the-location").build())
          .build()
        val startQueryExecutionResponse: StartQueryExecutionResponse = StartQueryExecutionResponse.builder()
          .queryExecutionId("query-execution-id").build()
        val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId("query-execution-id").build()
        given(athenaClient.startQueryExecution(startQueryExecutionRequest)).willReturn(startQueryExecutionResponse)
        given(athenaClient.getQueryExecution(getQueryExecutionRequest)).willReturn(
          GetQueryExecutionResponse.builder()
            .queryExecution(QueryExecution.builder().status(QueryExecutionStatus.builder().state(SUCCEEDED).build()).build()).build(),
        )

        val resultSet = ResultSet.builder()
          .resultSetMetadata(
            ResultSetMetadata.builder()
              .columnInfo(
                columnInfo("id"),
                columnInfo("what"),
                columnInfo("when"),
                columnInfo("operationId"),
                columnInfo("subjectId"),
                columnInfo("subjectType"),
                columnInfo("correlationId"),
                columnInfo("who"),
                columnInfo("service"),
                columnInfo("details"),
              )
              .build(),
          )
          .rows(
            listOf(
              Row.builder().data(
                Datum.builder().varCharValue(expectedAuditDto.id.toString()).build(),
                Datum.builder().varCharValue(expectedAuditDto.what).build(),
                Datum.builder().varCharValue(expectedAuditDto.`when`.toString()).build(),
                Datum.builder().varCharValue(expectedAuditDto.operationId).build(),
                Datum.builder().varCharValue(expectedAuditDto.subjectId).build(),
                Datum.builder().varCharValue(expectedAuditDto.subjectType).build(),
                Datum.builder().varCharValue(expectedAuditDto.correlationId).build(),
                Datum.builder().varCharValue(expectedAuditDto.who).build(),
                Datum.builder().varCharValue(expectedAuditDto.service).build(),
                Datum.builder().varCharValue(expectedAuditDto.details).build(),
              ).build(),
            ),
          ).build()
        val getQueryResultsRequest: GetQueryResultsRequest = GetQueryResultsRequest.builder().queryExecutionId("query-execution-id").build()
        whenever(athenaClient.getQueryResults(getQueryResultsRequest)).thenReturn(
          GetQueryResultsResponse.builder()
            .resultSet(resultSet)
            .build(),
        )

        webTestClient.post().uri("/audit/query")
          .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
          .body(
            BodyInserters.fromValue(
              DigitalServicesQueryRequest(
                startDate = LocalDate.of(2025, 1, 1),
                endDate = LocalDate.of(2025, 1, 31),
                subjectId = "test-subject",
                subjectType = "USER_ID",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            jacksonObjectMapper().registerModule(JavaTimeModule()).disable(WRITE_DATES_AS_TIMESTAMPS)
              .writeValueAsString(listOf(expectedAuditDto)),
          )
      }
    }
    private fun String.loadJson(): String = AuditResourceTest::class.java.getResource("$this.json")!!.readText()
    private fun columnInfo(name: String): ColumnInfo = ColumnInfo.builder().name(name).type("string").build()
  }
}
