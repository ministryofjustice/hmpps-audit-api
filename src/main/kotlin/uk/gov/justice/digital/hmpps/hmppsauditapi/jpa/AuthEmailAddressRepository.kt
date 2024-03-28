package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthEmailAddress

@Repository
interface AuthEmailAddressRepository : CrudRepository<AuthEmailAddress, Long> {
  fun findAllByEmailAddress(emailAddress: String): List<AuthEmailAddress>
}
