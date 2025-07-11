package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.util.UUID

data class DigitalServicesQueryResponse(
  val queryExecutionId: UUID,
  val queryState: QueryExecutionState,
  val authorisedServices: List<String>,
  val query: String = "",
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var results: List<AuditDto>? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var executionTimeInMillis: Long? = null,
)
