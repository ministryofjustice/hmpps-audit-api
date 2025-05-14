package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse

@ExtendWith(MockitoExtension::class)
class AthenaPartitionRepairServiceTest {

  @Mock
  private lateinit var athenaClient: AthenaClient

  private val databaseName = "databaseName"
  private val tableName = "tableName"
  private val workGroupName = "workGroupName"
  private val outputLocation = "outputLocation"

  private lateinit var service: AthenaPartitionRepairService

  private val startQueryExecutionRequestBuilder: StartQueryExecutionRequest.Builder = StartQueryExecutionRequest.builder()
    .queryExecutionContext(QueryExecutionContext.builder().database(databaseName).build())
    .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
    .workGroup(workGroupName)
  private val updatePartitionsQueryExecutionId = "d9906078-2776-46cc-bcfe-3f91cfbc181b"

  @BeforeEach
  fun setup() {
    service = AthenaPartitionRepairService(athenaClient, databaseName, tableName, workGroupName, outputLocation)
  }

  @Test
  fun `should repair Athena partitions and send telemetry`() {
    // Given
    val updatePartitionsQuery = "MSCK REPAIR TABLE $databaseName.$tableName;"
    val startQueryExecutionRequest = startQueryExecutionRequestBuilder.queryString(updatePartitionsQuery).build()
    given(athenaClient.startQueryExecution(startQueryExecutionRequest)).willReturn(StartQueryExecutionResponse.builder().queryExecutionId(updatePartitionsQueryExecutionId).build())

    // When
    service.repairPartitions()

    // Then
    verify(athenaClient).startQueryExecution(startQueryExecutionRequest)
  }
}
