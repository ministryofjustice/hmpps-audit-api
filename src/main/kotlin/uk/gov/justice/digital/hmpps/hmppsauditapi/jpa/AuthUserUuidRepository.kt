package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserUuid

// TODO test
@Repository
interface AuthUserUuidRepository : CrudRepository<AuthUserUuid, Long> {
  fun findAllByUuid(uuid: String): List<AuthUserUuid>
}
