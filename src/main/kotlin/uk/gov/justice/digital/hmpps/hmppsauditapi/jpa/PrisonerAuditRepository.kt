package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PrisonerAuditEvent
import java.time.Instant
import java.util.UUID

@Repository
interface PrisonerAuditRepository :
  PagingAndSortingRepository<PrisonerAuditEvent, UUID>,
  CrudRepository<PrisonerAuditEvent, UUID> {

  @Query(
    """
    select
        ae from PrisonerAuditEvent ae
    where
        ((cast(:startDate as string) is null or ae.when >=:startDate )
           and (cast(:endDate as string) is null or ae.when <=:endDate))
     and (cast(:service as string) is null or ae.service =:service)
     and (cast(:what as string) is null or ae.what =:what)
     and (cast(:subjectId as string) is null or ae.subjectId =:subjectId)
     and (cast(:subjectType as string) is null or ae.subjectType =:subjectType)
     and (cast(:correlationId as string) is null or ae.correlationId =:correlationId)
     and (cast(:who as string) is null or ae.who =:who)
    """,
  )
  fun findPage(
    pageable: Pageable,
    startDate: Instant?,
    endDate: Instant?,
    service: String?,
    subjectId: String?,
    subjectType: String?,
    correlationId: String?,
    what: String?,
    who: String?,
  ): Page<PrisonerAuditEvent>
}
