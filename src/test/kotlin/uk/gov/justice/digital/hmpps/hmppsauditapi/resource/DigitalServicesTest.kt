package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.json.JsonCompareMode.STRICT
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
import software.amazon.awssdk.services.athena.model.QueryExecutionStatistics
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.ResultSetMetadata
import software.amazon.awssdk.services.athena.model.Row
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DigitalServicesTest : IntegrationTest() {

  @Autowired
  private lateinit var athenaClient: AthenaClient

  private final val expectedAuditDto = AuditDto(
    id = UUID.fromString("cebcfc92-bdd6-4c3c-be50-a33fb08a9853"),
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

  private final val resultSet = ResultSet.builder()
    .resultSetMetadata(
      ResultSetMetadata.builder()
        .columnInfo(
          columnInfo("id"),
          columnInfo("what"),
          columnInfo("when"),
          columnInfo("operationid"),
          columnInfo("subjectid"),
          columnInfo("subjecttype"),
          columnInfo("correlationid"),
          columnInfo("who"),
          columnInfo("service"),
          columnInfo("details"),
        )
        .build(),
    )
    .rows(
      listOf(
        Row.builder().data(
          Datum.builder().varCharValue("id").build(),
          Datum.builder().varCharValue("what").build(),
          Datum.builder().varCharValue("when").build(),
          Datum.builder().varCharValue("operationId").build(),
          Datum.builder().varCharValue("subjectId").build(),
          Datum.builder().varCharValue("subjectType").build(),
          Datum.builder().varCharValue("correlationId").build(),
          Datum.builder().varCharValue("who").build(),
          Datum.builder().varCharValue("service").build(),
          Datum.builder().varCharValue("details").build(),
        ).build(),
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
  private val startQueryExecutionRequest: StartQueryExecutionRequest = StartQueryExecutionRequest.builder()
    .queryString("SELECT * FROM the-database.the-table WHERE ((year = '2025' AND month = '1' AND day = '1') OR (year = '2025' AND month = '1' AND day = '2') OR (year = '2025' AND month = '1' AND day = '3') OR (year = '2025' AND month = '1' AND day = '4') OR (year = '2025' AND month = '1' AND day = '5')) AND DATE(from_iso8601_timestamp(\"when\")) BETWEEN DATE '2025-01-01' AND DATE '2025-01-05' AND subjectId = 'test-subject' AND subjectType = 'USER_ID' AND service IN ('hmpps-manage-users');")
    .queryExecutionContext(QueryExecutionContext.builder().database("the-database").build())
    .workGroup("the-workgroup")
    .resultConfiguration(ResultConfiguration.builder().outputLocation("the-location").build())
    .build()
  private final val queryExecutionId = "b1231f6e-9653-4b3f-9507-793730932daf"
  private val startQueryExecutionResponse: StartQueryExecutionResponse = StartQueryExecutionResponse.builder().queryExecutionId(queryExecutionId).build()
  private final val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build()
  private final val successfulGetQueryExecutionResponse = GetQueryExecutionResponse.builder()
    .queryExecution(
      QueryExecution.builder()
        .statistics(QueryExecutionStatistics.builder().totalExecutionTimeInMillis(100).build())
        .status(QueryExecutionStatus.builder().state(SUCCEEDED).build()).build(),
    )
    .build()
  private final val getQueryResultsRequest: GetQueryResultsRequest = GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build()
  private final val getQueryResultsResponse = GetQueryResultsResponse.builder().resultSet(resultSet).build()

  @Test
  fun startQuery() {
    given(athenaClient.startQueryExecution(startQueryExecutionRequest)).willReturn(startQueryExecutionResponse)

    webTestClient.post().uri("/audit/query")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT", "ROLE_QUERY_AUDIT__HMPPS_MANAGE_USERS"), scopes = listOf("read")))
      .body(
        BodyInserters.fromValue(
          DigitalServicesQueryRequest(
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 1, 5),
            subjectId = "test-subject",
            subjectType = "USER_ID",
          ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody().json("start_query_response".loadJson(), STRICT)
  }

  @Test
  fun invalidQuery() {
    webTestClient.post().uri("/audit/query")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT", "ROLE_QUERY_AUDIT__HMPPS_MANAGE_USERS"), scopes = listOf("read")))
      .body(
        BodyInserters.fromValue(
          DigitalServicesQueryRequest(),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody().json("start_query_response_invalid".loadJson(), STRICT)
  }

  @Test
  fun getQueryResults() {
    given(athenaClient.getQueryExecution(getQueryExecutionRequest)).willReturn(successfulGetQueryExecutionResponse)
    given(athenaClient.getQueryResults(getQueryResultsRequest)).willReturn(getQueryResultsResponse)

    webTestClient.get().uri("/audit/query/{queryExecutionId}", queryExecutionId)
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT", "ROLE_QUERY_AUDIT__HMPPS_MANAGE_USERS"), scopes = listOf("read")))
      .exchange()
      .expectStatus().isOk
      .expectBody().json("get_query_results_response".loadJson(), STRICT)
  }
  private fun columnInfo(name: String): ColumnInfo = ColumnInfo.builder().name(name).type("string").build()
  private fun String.loadJson(): String = AuditResourceTest::class.java.getResource("$this.json")!!.readText()
}
