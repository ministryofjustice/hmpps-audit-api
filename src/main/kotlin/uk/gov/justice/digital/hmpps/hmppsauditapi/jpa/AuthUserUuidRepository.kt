package uk.gov.justice.digital.hmpps.hmppsauditapi.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserUuid
import java.util.UUID

// TODO test
@Repository
interface AuthUserUuidRepository : CrudRepository<AuthUserUuid, Long> {
  fun findAllByUserUuid(userUuid: UUID): List<AuthUserUuid>
}
