package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.ColumnInfo
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.ResultSetMetadata
import software.amazon.awssdk.services.athena.model.Row
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesAuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.time.Instant
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuditAthenaClientTest {

  private val databaseName = "databaseName"
  private val workGroupName = "workGroupName"
  private val outputLocation = "outputLocation"

  @Mock
  private lateinit var athenaClient: AthenaClient

  @BeforeEach
  fun setup() {
    auditAthenaClient = AuditAthenaClient(athenaClient, databaseName, workGroupName, outputLocation)
  }

  private lateinit var auditAthenaClient: AuditAthenaClient

  private val startQueryExecutionRequest: StartQueryExecutionRequest = StartQueryExecutionRequest.builder()
    .queryString("SELECT * FROM databaseName.audit_event WHERE `when` >= '-1000000000-01-01T00:00:00Z' AND `when` <= '-1000000000-01-01T00:00:30Z' AND who = 'someone' AND subjectId = 'subjectId' AND subjectType = 'subjectType';")
    .queryExecutionContext(QueryExecutionContext.builder().database(databaseName).build())
    .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
    .workGroup(workGroupName)
    .build()

  @Test
  fun `queryEvents should return results from Athena`() {
    // Given
    val queryExecutionId = "test-query-id"
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
    val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId("test-query-id").build()
    val getQueryResultsRequest: GetQueryResultsRequest = GetQueryResultsRequest.builder().queryExecutionId("test-query-id").build()

    whenever(athenaClient.startQueryExecution(startQueryExecutionRequest)).thenReturn(
      StartQueryExecutionResponse.builder().queryExecutionId(queryExecutionId).build(),
    )

    whenever(athenaClient.getQueryExecution(getQueryExecutionRequest)).thenReturn(
      GetQueryExecutionResponse.builder()
        .queryExecution(
          QueryExecution.builder().status(
            QueryExecutionStatus.builder().state(
              QueryExecutionState.SUCCEEDED,
            ).build(),
          ).build(),
        ).build(),
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

    whenever(athenaClient.getQueryResults(getQueryResultsRequest)).thenReturn(
      GetQueryResultsResponse.builder()
        .resultSet(resultSet)
        .build(),
    )

    // When
    val results = auditAthenaClient.queryEvents(
      DigitalServicesAuditFilterDto(
        startDateTime = Instant.MIN,
        endDateTime = Instant.MIN.plusSeconds(30),
        who = "someone",
        subjectId = "subjectId",
        subjectType = "subjectType",
      ),
    )

    // Then
    assertThat(results).hasSize(1)
    assertThat(results[0]).isEqualTo(expectedAuditDto)
  }

  @Test
  fun `queryEvents should throw exception if query fails`() {
    // Given
    val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId("test-query-id").build()
    val queryExecutionId = "test-query-id"
    whenever(athenaClient.startQueryExecution(startQueryExecutionRequest)).thenReturn(
      StartQueryExecutionResponse.builder().queryExecutionId(queryExecutionId).build(),
    )
    whenever(athenaClient.getQueryExecution(getQueryExecutionRequest)).thenReturn(
      GetQueryExecutionResponse.builder().queryExecution(
        QueryExecution.builder().status(
          QueryExecutionStatus.builder()
            .state(QueryExecutionState.FAILED)
            .build(),
        ).build(),
      ).build(),
    )

    // Then
    org.junit.jupiter.api.assertThrows<RuntimeException> {
      auditAthenaClient.queryEvents(DigitalServicesAuditFilterDto())
    }
  }

  private fun columnInfo(name: String): ColumnInfo = ColumnInfo.builder().name(name).type("string").build()
}
