package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.util.UUID

data class AthenaQueryResponse(
  val queryExecutionId: UUID,
  var queryState: QueryExecutionState,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var authorisedServices: List<String>? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var results: List<AuditDto>? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var executionTimeInMillis: Long? = null,
)
