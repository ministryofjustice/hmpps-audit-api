package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUsername

// TODO test
@Repository
interface AuditUsernameRepository: CrudRepository<AuditUsername, Long> {
  fun findAllByUsername(username : String) : List<AuditUsername>
}
