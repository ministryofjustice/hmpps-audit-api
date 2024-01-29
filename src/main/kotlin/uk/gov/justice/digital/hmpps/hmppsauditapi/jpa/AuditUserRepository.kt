package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser

// TODO test
@Repository
interface AuditUserRepository : CrudRepository<AuditUser, Long>
