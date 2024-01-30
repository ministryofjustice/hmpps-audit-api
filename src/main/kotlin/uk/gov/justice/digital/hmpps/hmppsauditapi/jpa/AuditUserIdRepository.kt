package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUserId

@Repository
interface AuditUserIdRepository : CrudRepository<AuditUserId, Long> {
  fun findAllByUserId(userId: String): List<AuditUserId>
}
