package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.util.UUID

@Repository
interface AuditRepository : PagingAndSortingRepository<AuditEvent, UUID>
