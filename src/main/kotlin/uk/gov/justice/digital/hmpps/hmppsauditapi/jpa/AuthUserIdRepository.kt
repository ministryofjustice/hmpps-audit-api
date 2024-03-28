package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserId

@Repository
interface AuthUserIdRepository : CrudRepository<AuthUserId, Long> {
  fun findAllByUserId(userId: String): List<AuthUserId>
}
