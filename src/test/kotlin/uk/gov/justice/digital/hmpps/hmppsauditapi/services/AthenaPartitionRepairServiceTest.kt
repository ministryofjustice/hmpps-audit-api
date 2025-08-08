package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState.QUEUED
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryResponse
import java.util.UUID

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
  private val s3BucketName = "s3BucketName"

  private lateinit var service: AthenaPartitionRepairService

  private val startQueryExecutionRequestBuilder: StartQueryExecutionRequest.Builder =
    StartQueryExecutionRequest.builder()
      .queryExecutionContext(QueryExecutionContext.builder().database(databaseName).build())
      .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
      .workGroup(workGroupName)
  private val updatePartitionsQueryExecutionId = "d9906078-2776-46cc-bcfe-3f91cfbc181b"

  @BeforeEach
  fun setup() {
    service = AthenaPartitionRepairService(
      athenaClient,
      athenaPropertiesFactory,
    )
  }

  @ParameterizedTest
  @EnumSource(AuditEventType::class)
  fun `should repair Athena partitions`(auditEventType: AuditEventType) {
    // Given
    val updatePartitionsQuery = "MSCK REPAIR TABLE $databaseName.$tableName;"
    val startQueryExecutionRequest = startQueryExecutionRequestBuilder.queryString(updatePartitionsQuery).build()
    given(athenaClient.startQueryExecution(startQueryExecutionRequest)).willReturn(
      StartQueryExecutionResponse.builder().queryExecutionId(updatePartitionsQueryExecutionId).build(),
    )
    given(athenaPropertiesFactory.getProperties(auditEventType)).willReturn(
      AthenaProperties(
        auditEventType = auditEventType,
        databaseName = databaseName,
        tableName = tableName,
        workGroupName = workGroupName,
        outputLocation = outputLocation,
        s3BucketName = s3BucketName,
      ),
    )

    // When
    val queryResponse = service.triggerRepairPartitions(auditEventType)

    // Then
    assertThat(queryResponse).isEqualTo(
      AuditQueryResponse(
        queryExecutionId = UUID.fromString(updatePartitionsQueryExecutionId),
        queryState = QUEUED,
        authorisedServices = emptyList(),
      ),
    )
  }
}
