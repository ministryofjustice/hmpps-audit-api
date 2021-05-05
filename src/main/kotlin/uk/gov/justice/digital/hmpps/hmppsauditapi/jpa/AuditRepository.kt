package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.util.UUID

@Repository
interface AuditRepository : PagingAndSortingRepository<AuditEvent, UUID> {
  @Query(
    """
    select 
        ae from AuditEvent ae 
    where 
         (coalesce(:what) is null or ae.what=:what)
     and (coalesce(:who) is null or ae.who=:who)
    """
  )
  fun findPage(pageable: Pageable, who: String?, what: String?): Page<AuditEvent>
}
