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
        ((cast(:startDate as string) is null and cast(:endDate as string) is null)  
          or (cast(:startDate as string) is not null 
              and cast(:endDate as string) is not null
              and (ae.when between :startDate and :endDate)) 
          or (cast(:startDate as string) is not null 
              and cast(:endDate as string) is null
              and ae.when >=:startDate ) 
          or (cast(:startDate as string) is null 
              and cast(:endDate as string) is not null
              and ae.when <=:endDate )  
        )
     and (cast(:service as string) is null or ae.service =:service)
     and (cast(:what as string) is null or ae.what =:what)
     and (cast(:who as string) is null or ae.who =:who)
    """
  )
  fun findPage(
    pageable: Pageable,
    startDate: Instant?,
    endDate: Instant?,
    service: String?,
    what: String?,
    who: String?
  ): Page<AuditEvent>
}
