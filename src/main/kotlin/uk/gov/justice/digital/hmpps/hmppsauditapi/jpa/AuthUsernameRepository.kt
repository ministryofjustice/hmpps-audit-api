package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUsername

@Repository
interface AuthUsernameRepository : CrudRepository<AuthUsername, Long> {
  fun findAllByUsername(username: String): List<AuthUsername>
}
