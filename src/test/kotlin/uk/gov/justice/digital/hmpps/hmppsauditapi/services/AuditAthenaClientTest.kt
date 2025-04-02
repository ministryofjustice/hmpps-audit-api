package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
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
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

private const val ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS = "ROLE_QUERY_AUDIT__HMPPS_MANAGE_USERS"
private const val ROLE_QUERY_AUDIT__HMPPS_EXTERNAL_USERS = "ROLE_QUERY_AUDIT__HMPPS_EXTERNAL_USERS"
private const val HMPPS_MANAGE_USERS = "hmpps-manage-users"
private const val HMPPS_EXTERNAL_USERS = "hmpps-external-users"

@ExtendWith(MockitoExtension::class)
class AuditAthenaClientTest {
  private val databaseName = "databaseName"
  private val workGroupName = "workGroupName"
  private val outputLocation = "outputLocation"
  private val queryExecutionId = "a4ab5455-dfe1-46f2-917d-5135b7dadae3"
  private val updatePartitionsQueryExecutionId = "d9906078-2776-46cc-bcfe-3f91cfbc181b"
  private val successfulQueryExecutionResponse = GetQueryExecutionResponse.builder()
    .queryExecution(
      QueryExecution.builder().status(
        QueryExecutionStatus.builder().state(
          QueryExecutionState.SUCCEEDED,
        ).build(),
      ).build(),
    ).build()

  @Mock
  private lateinit var athenaClient: AthenaClient

  @BeforeEach
  fun setup() {
    auditAthenaClient = AuditAthenaClient(athenaClient, databaseName, workGroupName, outputLocation)
  }

  private lateinit var auditAthenaClient: AuditAthenaClient

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class TriggerQuery {
    private val startQueryExecutionRequestBuilder: StartQueryExecutionRequest.Builder = StartQueryExecutionRequest.builder()
      .queryExecutionContext(QueryExecutionContext.builder().database(databaseName).build())
      .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
      .workGroup(workGroupName)

    @ParameterizedTest
    @MethodSource("triggerQueryParameters")
    fun triggerQuery(digitalServicesQueryRequest: DigitalServicesQueryRequest, roles: List<String>, expectedQuery: String, expectedServices: List<String>) {
      // Given
      SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "credentials", roles.map { SimpleGrantedAuthority(it) })

      val updatePartitionsQuery = "MSCK REPAIR TABLE $databaseName.audit_event;"
      given(athenaClient.startQueryExecution(startQueryExecutionRequestBuilder.queryString(updatePartitionsQuery).build()))
        .willReturn(StartQueryExecutionResponse.builder().queryExecutionId(updatePartitionsQueryExecutionId).build())
      given(athenaClient.getQueryExecution(GetQueryExecutionRequest.builder().queryExecutionId(updatePartitionsQueryExecutionId).build()))
        .willReturn(successfulQueryExecutionResponse)
      given(athenaClient.startQueryExecution(startQueryExecutionRequestBuilder.queryString(expectedQuery).build()))
        .willReturn(StartQueryExecutionResponse.builder().queryExecutionId(queryExecutionId).build())

      // When
      val response: DigitalServicesQueryResponse = auditAthenaClient.triggerQuery(digitalServicesQueryRequest)

      // Then
      assertThat(response.queryExecutionId).isEqualTo(UUID.fromString(queryExecutionId))
      assertThat(response.queryState).isEqualTo(QueryExecutionState.QUEUED)
      assertThat(response.authorisedServices).containsExactlyInAnyOrderElementsOf(expectedServices)
    }

    private fun triggerQueryParameters(): Stream<Arguments> = Stream.of(
      // All fields
      Arguments.of(
        DigitalServicesQueryRequest(
          startDate = LocalDate.of(2025, 1, 1),
          endDate = LocalDate.of(2025, 1, 31),
          who = "someone",
          subjectId = "subjectId",
          subjectType = "subjectType",
        ),
        listOf(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS),
        "SELECT * FROM databaseName.audit_event WHERE DATE(from_iso8601_timestamp(\"when\")) BETWEEN DATE '2025-01-01' AND DATE '2025-01-31' AND who = 'someone' AND subjectId = 'subjectId' AND subjectType = 'subjectType' AND service IN ('hmpps-manage-users');",
        listOf(HMPPS_MANAGE_USERS),
      ),

      // Subject, no who
      Arguments.of(
        DigitalServicesQueryRequest(
          startDate = LocalDate.of(2025, 1, 1),
          endDate = LocalDate.of(2025, 1, 31),
          subjectId = "subjectId",
          subjectType = "subjectType",
        ),
        listOf(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS, ROLE_QUERY_AUDIT__HMPPS_EXTERNAL_USERS),
        "SELECT * FROM databaseName.audit_event WHERE DATE(from_iso8601_timestamp(\"when\")) BETWEEN DATE '2025-01-01' AND DATE '2025-01-31' AND subjectId = 'subjectId' AND subjectType = 'subjectType' AND service IN ('hmpps-manage-users', 'hmpps-external-users');",
        listOf(HMPPS_MANAGE_USERS, HMPPS_EXTERNAL_USERS),
      ),

      // Subject + endDate, no who, no startDate
      Arguments.of(
        DigitalServicesQueryRequest(
          startDate = LocalDate.of(2025, 1, 1),
          subjectId = "subjectId",
          subjectType = "subjectType",
        ),
        listOf(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS),
        "SELECT * FROM databaseName.audit_event WHERE DATE(from_iso8601_timestamp(\"when\")) >= DATE '2025-01-01' AND subjectId = 'subjectId' AND subjectType = 'subjectType' AND service IN ('hmpps-manage-users');",
        listOf(HMPPS_MANAGE_USERS),
      ),

      // Subject + endDate, no who, no startDate
      Arguments.of(
        DigitalServicesQueryRequest(
          endDate = LocalDate.of(2025, 1, 31),
          subjectId = "subjectId",
          subjectType = "subjectType",
        ),
        listOf(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS),
        "SELECT * FROM databaseName.audit_event WHERE DATE(from_iso8601_timestamp(\"when\")) <= DATE '2025-01-31' AND subjectId = 'subjectId' AND subjectType = 'subjectType' AND service IN ('hmpps-manage-users');",
        listOf(HMPPS_MANAGE_USERS),
      ),

      // StartDate + endDate + who, no subject
      Arguments.of(
        DigitalServicesQueryRequest(
          startDate = LocalDate.of(2025, 1, 1),
          endDate = LocalDate.of(2025, 1, 31),
          who = "someone",
        ),
        listOf(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS),
        "SELECT * FROM databaseName.audit_event WHERE DATE(from_iso8601_timestamp(\"when\")) BETWEEN DATE '2025-01-01' AND DATE '2025-01-31' AND who = 'someone' AND service IN ('hmpps-manage-users');",
        listOf(HMPPS_MANAGE_USERS),
      ),

      // startDate + who, no subject, no endDate
      Arguments.of(
        DigitalServicesQueryRequest(
          startDate = LocalDate.of(2025, 1, 1),
          who = "someone",
        ),
        listOf(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS),
        "SELECT * FROM databaseName.audit_event WHERE DATE(from_iso8601_timestamp(\"when\")) >= DATE '2025-01-01' AND who = 'someone' AND service IN ('hmpps-manage-users');",
        listOf(HMPPS_MANAGE_USERS),
      ),

      // No authorised services
      Arguments.of(
        DigitalServicesQueryRequest(
          startDate = LocalDate.of(2025, 1, 1),
          who = "someone",
        ),
        emptyList<String>(),
        "SELECT * FROM databaseName.audit_event WHERE 1 = 0;",
        emptyList<String>(),
      ),
    )
  }

  @Nested
  inner class GetQueryResults {
    private val expectedAuditDto = AuditDto(
      id = UUID.randomUUID(),
      what = "READ_USER",
      `when` = Instant.parse("2025-01-14T12:34:56Z"),
      operationId = "op-123",
      subjectId = "sub-456",
      subjectType = "User",
      correlationId = "corr-789",
      who = "test-user",
      service = "auth-service",
      details = "some details",
    )
    private val resultSet = ResultSet.builder()
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
    private val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build()
    private val getQueryResultsRequest: GetQueryResultsRequest = GetQueryResultsRequest.builder().queryExecutionId(queryExecutionId).build()
    private val getQueryResultsResponse = GetQueryResultsResponse.builder().resultSet(resultSet).build()

    @Test
    fun getQueryResults() {
      // Given
      val authorities = listOf(SimpleGrantedAuthority(ROLE_QUERY_AUDIT_HMPPS_MANAGE_USERS))
      SecurityContextHolder.getContext().authentication =
        TestingAuthenticationToken("user", "credentials", authorities)

      given(athenaClient.getQueryExecution(getQueryExecutionRequest)).willReturn(successfulQueryExecutionResponse)
      given(athenaClient.getQueryResults(getQueryResultsRequest)).willReturn(getQueryResultsResponse)

      // When
      val digitalServicesAuditQueryResponse = auditAthenaClient.getQueryResults(queryExecutionId)

      // Then
      assertThat(digitalServicesAuditQueryResponse).isEqualTo(
        DigitalServicesQueryResponse(
          queryExecutionId = UUID.fromString(queryExecutionId),
          queryState = QueryExecutionState.SUCCEEDED,
          results = listOf(expectedAuditDto),
          authorisedServices = listOf(HMPPS_MANAGE_USERS),
        ),
      )
    }
  }

  private fun columnInfo(name: String): ColumnInfo = ColumnInfo.builder().name(name).type("string").build()
}
