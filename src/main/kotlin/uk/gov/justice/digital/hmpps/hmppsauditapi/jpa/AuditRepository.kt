package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.time.Instant
import java.util.UUID

@Repository
interface AuditRepository : PagingAndSortingRepository<AuditEvent, UUID> {
  @Query(
    """
    select 
        ae from AuditEvent ae 
    where
         (:what is null or ae.what=:what)
     and (:who is null or ae.who=:who)
    """
  )
  fun findPage(pageable: Pageable, who: String?, what: String?): Page<AuditEvent>

  @Query(
    """
    select 
        ae from AuditEvent ae 
    where
        ((cast(:startDate as string) is null or cast(:endDate as string) is null) or (ae.when between :startDate and :endDate) )
     and (cast(:service as string) is null or ae.service like concat('%',:service,'%'))
     and (cast(:what as string) is null or ae.what like concat('%',:what,'%'))
     and (cast(:who as string) is null or ae.who like concat('%',:who,'%'))
    """
  )
  fun findFilteredResults(pageable: Pageable, startDate: Instant?, endDate: Instant?, service: String?, what: String?, who: String?): Page<AuditEvent>
}
