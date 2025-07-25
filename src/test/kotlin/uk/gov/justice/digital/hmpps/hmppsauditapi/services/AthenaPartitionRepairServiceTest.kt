package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@ExtendWith(MockitoExtension::class)
class AthenaPartitionRepairServiceTest {

  @Mock
  private lateinit var athenaClient: AthenaClient

  @Mock
  private lateinit var athenaPropertiesFactory: AthenaPropertiesFactory

  private val databaseName = "databaseName"
  private val tableName = "tableName"
  private val workGroupName = "workGroupName"
  private val outputLocation = "outputLocation"
  private val prisonerDatabaseName = "prisonerDatabaseName"
  private val prisonerTableName = "prisonerTableName"
  private val prisonerWorkGroupName = "prisonerWorkGroupName"
  private val prisonerOutputLocation = "prisonerOutputLocation"

  private lateinit var service: AthenaPartitionRepairService

  private val startQueryExecutionRequestBuilder: StartQueryExecutionRequest.Builder =
    StartQueryExecutionRequest.builder()
      .queryExecutionContext(QueryExecutionContext.builder().database(databaseName).build())
      .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
      .workGroup(workGroupName)
  private val updatePartitionsQueryExecutionId = "d9906078-2776-46cc-bcfe-3f91cfbc181b"

  private val startPrisonerQueryExecutionRequestBuilder: StartQueryExecutionRequest.Builder =
    StartQueryExecutionRequest.builder()
      .queryExecutionContext(QueryExecutionContext.builder().database(prisonerDatabaseName).build())
      .resultConfiguration(ResultConfiguration.builder().outputLocation(prisonerOutputLocation).build())
      .workGroup(prisonerWorkGroupName)
  private val updatePrisonerPartitionsQueryExecutionId = "d9906078-2776-46cc-bcfe-aaaabbbbcccc"

  @BeforeEach
  fun setup() {
    service = AthenaPartitionRepairService(
      athenaClient,
      athenaPropertiesFactory,
    )
  }

  @Test
  fun `should repair Athena partitions and send telemetry`() {
    // Given
    val updatePartitionsQuery = "MSCK REPAIR TABLE $databaseName.$tableName;"
    val startQueryExecutionRequest = startQueryExecutionRequestBuilder.queryString(updatePartitionsQuery).build()
    given(athenaClient.startQueryExecution(startQueryExecutionRequest)).willReturn(
      StartQueryExecutionResponse.builder().queryExecutionId(updatePartitionsQueryExecutionId).build(),
    )

    // When
    whenever(athenaPropertiesFactory.getProperties(AuditEventType.STAFF)).thenReturn(
      AthenaProperties(
        databaseName = "databaseName",
        tableName = "tableName",
        workGroupName = "workGroupName",
        outputLocation = "outputLocation",
      ),
    )
    service.repairPartitions(AuditEventType.STAFF)

    // Then
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }

  @Test
  fun `should repair Prisoner Athena partitions and send telemetry`() {
    // Given
    val updatePartitionsQuery = "MSCK REPAIR TABLE $prisonerDatabaseName.$prisonerTableName;"
    val startQueryExecutionRequest = startPrisonerQueryExecutionRequestBuilder.queryString(updatePartitionsQuery).build()
    given(athenaClient.startQueryExecution(startQueryExecutionRequest)).willReturn(
      StartQueryExecutionResponse.builder().queryExecutionId(updatePrisonerPartitionsQueryExecutionId).build(),
    )

    // When
    whenever(athenaPropertiesFactory.getProperties(AuditEventType.PRISONER)).thenReturn(
      AthenaProperties(
        databaseName = "prisonerDatabaseName",
        tableName = "prisonerTableName",
        workGroupName = "prisonerWorkGroupName",
        outputLocation = "prisonerOutputLocation",
      ),
    )
    service.repairPartitions(AuditEventType.PRISONER)

    // Then
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }
}
