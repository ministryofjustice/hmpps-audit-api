package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.json.JsonCompareMode.STRICT
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionState.SUCCEEDED
import software.amazon.awssdk.services.athena.model.QueryExecutionStatistics
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import java.util.UUID

class PartitionRepairControllerTest : IntegrationTest() {

  private val queryExecutionId = UUID.randomUUID().toString()

  @Autowired
  private lateinit var athenaClient: AthenaClient

  @ParameterizedTest
  @EnumSource(value = AuditEventType::class)
  fun shouldTriggerPartitionRepair(auditEventType: AuditEventType) {
    given(athenaClient.startQueryExecution(any<StartQueryExecutionRequest>())).willReturn(
      StartQueryExecutionResponse.builder()
        .queryExecutionId(queryExecutionId)
        .build(),
    )

    webTestClient.post().uri("/audit/$auditEventType/partition-repair")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
      .exchange()
      .expectStatus().isOk
      .expectBody().json("{\"queryExecutionId\":\"$queryExecutionId\",\"queryState\":\"QUEUED\",\"authorisedServices\":[]}", STRICT)
  }

  @Test
  fun getRepairPartitionsResults() {
    val queryExecutionResponse = GetQueryExecutionResponse.builder().queryExecution(
      QueryExecution.builder().queryExecutionId(queryExecutionId)
        .statistics(QueryExecutionStatistics.builder().totalExecutionTimeInMillis(123456).build()).status(
          QueryExecutionStatus.builder().state(SUCCEEDED).build(),
        ).build(),
    ).build()
    given(athenaClient.getQueryExecution(any<GetQueryExecutionRequest>())).willReturn(queryExecutionResponse)
    webTestClient.get().uri("/audit/query/partition-repair/$queryExecutionId")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
      .exchange()
      .expectStatus().isOk
      .expectBody().json("{\"queryExecutionId\":\"$queryExecutionId\",\"queryState\":\"SUCCEEDED\",\"authorisedServices\":[],\"executionTimeInMillis\":123456}", STRICT)
  }
}
