package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditEmailAddress

@Repository
interface AuditEmailAddressRepository : CrudRepository<AuditEmailAddress, Long> {
  fun findAllByEmailAddress(emailAddress: String): List<AuditEmailAddress>
}
