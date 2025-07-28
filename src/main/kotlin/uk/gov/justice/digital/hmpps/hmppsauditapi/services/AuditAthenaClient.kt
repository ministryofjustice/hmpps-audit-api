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
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private const val AUTHORISED_SERVICE_ROLE_PREFIX = "ROLE_QUERY_AUDIT__"

@Service
class AuditAthenaClient(
  private val athenaClient: AthenaClient,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
  private val clock: Clock,
  @Value("\${hmpps.audit.queriesStartDate}") private val queriesStartDate: LocalDate,
) {

  fun triggerQuery(filter: DigitalServicesQueryRequest, auditEventType: AuditEventType): AthenaQueryResponse {
    if (filter.startDate == null) {
      filter.startDate = queriesStartDate
    }
    if (filter.endDate == null) {
      filter.endDate = LocalDate.now(clock)
    }
    val authorisedServices = getAuthorisedServices()
    val query = buildAthenaQuery(filter, authorisedServices, auditEventType)
    val queryExecutionId = startAthenaQuery(query, auditEventType)

    return AthenaQueryResponse(
      queryExecutionId = UUID.fromString(queryExecutionId),
      queryState = QueryExecutionState.QUEUED,
      authorisedServices = authorisedServices,
    )
  }

  fun getAuditEventsQueryResults(queryExecutionId: String): AthenaQueryResponse {
    val response = getQueryResults(queryExecutionId)
    if (response.queryState == QueryExecutionState.SUCCEEDED) {
      response.results = fetchQueryResults(queryExecutionId)
    }
    return response
  }

  fun getQueryResults(queryExecutionId: String): AthenaQueryResponse {
    val queryExecution = athenaClient.getQueryExecution(GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build()).queryExecution()
    val queryState = queryExecution.status().state()
    val response = AthenaQueryResponse(
      queryExecutionId = UUID.fromString(queryExecutionId),
      queryState = queryState,
      authorisedServices = getAuthorisedServices(),
      executionTimeInMillis = queryExecution.statistics().totalExecutionTimeInMillis(),
    )
    return response
  }

  private fun buildAthenaQuery(filter: DigitalServicesQueryRequest, services: List<String>, auditEventType: AuditEventType): String {
    val athenaProperties = athenaPropertiesFactory.getProperties(auditEventType)
    val conditions = mutableListOf<String>()
    val databaseName = athenaProperties.databaseName
    val tableName = athenaProperties.tableName

    if (services.isEmpty()) {
      return "SELECT * FROM $databaseName.$tableName WHERE 1 = 0;"
    }

    // Partition filtering based on full year/month/day decomposition
    val partitionConditions = buildPartitionDateConditions(filter.startDate!!, filter.endDate!!)
    conditions.add("(${partitionConditions.joinToString(" OR ")})")

    // Timestamp-based filtering for precision
    conditions.add("DATE(from_iso8601_timestamp(\"when\")) BETWEEN DATE '${filter.startDate}' AND DATE '${filter.endDate}'")
    filter.who?.let { conditions.add("user = '$it'") }
    filter.subjectId?.let { conditions.add("subjectId = '$it'") }
    filter.subjectType?.let { conditions.add("subjectType = '$it'") }

    if (userDoesNotHaveAccessToAllServices(services)) {
      val serviceList = services.joinToString(", ") { "'$it'" }
      conditions.add("service IN ($serviceList)")
    }

    val whereClause = "WHERE ${conditions.joinToString(" AND ")}"

    return "SELECT * FROM $databaseName.$tableName $whereClause;"
  }

  private fun buildPartitionDateConditions(startDate: LocalDate, endDate: LocalDate): List<String> {
    require(!endDate.isBefore(startDate)) { "End date must be on or after start date" }

    return generateSequence(startDate) { it.plusDays(1) }
      .takeWhile { !it.isAfter(endDate) }
      .map { date ->
        "(year = '${date.year}' AND month = '${date.monthValue}' AND day = '${date.dayOfMonth}')"
      }
      .toList()
  }

  private fun startAthenaQuery(query: String, auditEventType: AuditEventType): String {
    val athenaProperties = athenaPropertiesFactory.getProperties(auditEventType)
    val request = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext { it.database(athenaProperties.databaseName) }
      .workGroup(athenaProperties.workGroupName)
      .resultConfiguration { it.outputLocation(athenaProperties.outputLocation) }
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

  private fun userDoesNotHaveAccessToAllServices(services: List<String>) = !services.any { it.equals("all-services", ignoreCase = true) }
}
