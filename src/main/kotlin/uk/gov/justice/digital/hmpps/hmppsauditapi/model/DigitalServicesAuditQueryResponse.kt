package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import software.amazon.awssdk.services.athena.model.QueryExecutionState
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.util.UUID

data class DigitalServicesAuditQueryResponse(
  val queryId: UUID,
  val queryState: QueryExecutionState,
  val results: List<AuditDto>? = null,
)
