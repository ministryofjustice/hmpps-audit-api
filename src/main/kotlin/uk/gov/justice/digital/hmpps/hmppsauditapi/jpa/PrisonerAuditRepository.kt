package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PrisonerAuditEvent
import java.time.Instant
import java.util.UUID
import java.util.stream.Stream

@Repository
interface PrisonerAuditRepository :
  PagingAndSortingRepository<PrisonerAuditEvent, UUID>,
  CrudRepository<PrisonerAuditEvent, UUID> {

  @QueryHints(
    value = [
      QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "1000"),
      QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true"),
    ],
  )
  fun findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start: Instant, end: Instant): Stream<PrisonerAuditEvent>

  /** For dev integration tests only (DevTestingController); not for use in application code. */
  fun findAllByWhatAndCorrelationIdAndWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(
    what: String,
    correlationId: String,
    start: Instant,
    end: Instant,
  ): List<PrisonerAuditEvent>

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
