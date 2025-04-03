package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.time.Instant
import java.util.UUID

private const val AUTHORISED_SERVICE_ROLE_PREFIX = "ROLE_QUERY_AUDIT__"

@Service
class AuditAthenaClient(
  private val athenaClient: AthenaClient,
  @Value("\${aws.athena.database}") private val databaseName: String,
  @Value("\${aws.athena.workgroup}") private val workGroup: String,
  @Value("\${aws.athena.outputLocation}") private val outputLocation: String,
) {

  fun triggerQuery(filter: DigitalServicesQueryRequest): DigitalServicesQueryResponse {
    updateAthenaPartitions()
    val authorisedServices = getAuthorisedServices()
    val query = buildAthenaQuery(filter, authorisedServices)
    val queryExecutionId = startAthenaQuery(query)

    return DigitalServicesQueryResponse(
      queryExecutionId = UUID.fromString(queryExecutionId),
      queryState = QueryExecutionState.QUEUED,
      authorisedServices = authorisedServices,
    )
  }

  fun getQueryResults(queryExecutionId: String): DigitalServicesQueryResponse {
    val queryState = athenaClient.getQueryExecution(
      GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build(),
    ).queryExecution().status().state()
    val response = DigitalServicesQueryResponse(
      queryExecutionId = UUID.fromString(queryExecutionId),
      queryState = queryState,
      authorisedServices = getAuthorisedServices(),
    )
    if (queryState == QueryExecutionState.SUCCEEDED) {
      response.results = fetchQueryResults(queryExecutionId)
    }
    return response
  }

  private fun updateAthenaPartitions() {
    val repairTableQuery = "MSCK REPAIR TABLE $databaseName.audit_event;"

    val request = StartQueryExecutionRequest.builder()
      .queryString(repairTableQuery)
      .queryExecutionContext { it.database(databaseName) }
      .workGroup(workGroup)
      .resultConfiguration { it.outputLocation(outputLocation) }
      .build()

    val response = athenaClient.startQueryExecution(request)
    val queryExecutionId = response.queryExecutionId()

    waitForQueryToComplete(queryExecutionId)
  }

  private fun waitForQueryToComplete(queryExecutionId: String) {
    while (true) {
      val queryStatus = athenaClient.getQueryExecution(
        GetQueryExecutionRequest.builder()
          .queryExecutionId(queryExecutionId)
          .build(),
      ).queryExecution().status().state()

      when (queryStatus) {
        QueryExecutionState.SUCCEEDED -> return
        QueryExecutionState.FAILED, QueryExecutionState.CANCELLED ->
          throw RuntimeException("Athena query to update partitions failed with state: $queryStatus")
        else -> {
          Thread.sleep(2000)
        }
      }
    }
  }

  private fun buildAthenaQuery(filter: DigitalServicesQueryRequest, services: List<String>): String {
    val conditions = mutableListOf<String>()

    if (services.isEmpty()) {
      return "SELECT * FROM $databaseName.audit_event WHERE 1 = 0;"
    }

    if (filter.startDate != null && filter.endDate != null) {
      conditions.add("DATE(from_iso8601_timestamp(\"when\")) BETWEEN DATE '${filter.startDate}' AND DATE '${filter.endDate}'")
    } else if (filter.startDate != null) {
      conditions.add("DATE(from_iso8601_timestamp(\"when\")) >= DATE '${filter.startDate}'")
    } else if (filter.endDate != null) {
      conditions.add("DATE(from_iso8601_timestamp(\"when\")) <= DATE '${filter.endDate}'")
    }
    filter.who?.let { conditions.add("who = '$it'") }
    filter.subjectId?.let { conditions.add("subjectId = '$it'") }
    filter.subjectType?.let { conditions.add("subjectType = '$it'") }

    val serviceList = services.joinToString(", ") { "'$it'" }
    conditions.add("service IN ($serviceList)")

    val whereClause = "WHERE ${conditions.joinToString(" AND ")}"

    return "SELECT * FROM $databaseName.audit_event $whereClause;"
  }

  private fun startAthenaQuery(query: String): String {
    val request = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext { it.database(databaseName) }
      .workGroup(workGroup)
      .resultConfiguration { it.outputLocation(outputLocation) }
      .build()

    val response = athenaClient.startQueryExecution(request)
    return response.queryExecutionId()
  }

  private fun fetchQueryResults(queryExecutionId: String): List<AuditDto> {
    val request = GetQueryResultsRequest.builder()
      .queryExecutionId(queryExecutionId)
      .build()

    val results = mutableListOf<AuditDto>()
    var nextToken: String? = null

    do {
      val response = athenaClient.getQueryResults(
        request.toBuilder().nextToken(nextToken).build(),
      )

      val columnNames = response.resultSet().resultSetMetadata().columnInfo().map { it.name() }

      response.resultSet().rows().drop(1).forEach { row ->
        val values = row.data().map { it.varCharValue() ?: "" }
        val resultMap = columnNames.zip(values).toMap()

        val auditDto = AuditDto(
          id = UUID.fromString(resultMap["id"]),
          what = resultMap["what"] ?: "",
          `when` = Instant.parse(resultMap["when"] ?: throw IllegalArgumentException("Missing timestamp")),
          operationId = resultMap["operationId"],
          subjectId = resultMap["subjectId"],
          subjectType = resultMap["subjectType"],
          correlationId = resultMap["correlationId"],
          who = resultMap["who"],
          service = resultMap["service"],
          details = resultMap["details"],
        )

        results.add(auditDto)
      }

      nextToken = response.nextToken()
    } while (nextToken != null)

    return results
  }

  private fun getAuthorisedServices(): List<String> {
    val authentication = SecurityContextHolder.getContext().authentication
    return authentication?.authorities
      ?.map(GrantedAuthority::getAuthority)
      ?.filter { it.startsWith(AUTHORISED_SERVICE_ROLE_PREFIX) }
      ?.map { it.removePrefix(AUTHORISED_SERVICE_ROLE_PREFIX).lowercase().replace('_', '-') }
      ?: emptyList()
  }
}
