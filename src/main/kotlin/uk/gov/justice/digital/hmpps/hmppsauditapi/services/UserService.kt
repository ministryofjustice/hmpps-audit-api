package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditEmailAddressRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditUserIdRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditUserRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditUsernameRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditEmailAddress
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUserId
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUsername
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSUserListener.UserCreationEvent

// TODO test
@Service
class UserService(
  private val auditUserRepository: AuditUserRepository,
  private val auditUserIdRepository: AuditUserIdRepository,
  private val auditEmailAddressRepository: AuditEmailAddressRepository,
  private val auditUsernameRepository: AuditUsernameRepository,
) {

  @Transactional
  fun saveNewUserDetails(newUserDetails: UserCreationEvent) {
    checkUserDoesNotExist(newUserDetails)
    saveRecords(newUserDetails)
  }

  // TODO userUUID too?
  private fun checkUserDoesNotExist(newUserDetails: UserCreationEvent) {
    val auditUserIds: List<AuditUserId> = auditUserIdRepository.findAllByUserId(newUserDetails.userId)
    val auditEmailAddresses: List<AuditEmailAddress> = auditEmailAddressRepository.findAllByEmailAddress(newUserDetails.emailAddress)
    val auditUsernames: List<AuditUsername> = auditUsernameRepository.findAllByUsername(newUserDetails.username)

    if (auditUserIds.isNotEmpty()) {
      throw RuntimeException("User with user ID ${newUserDetails.userId} already exists")
    }
    if (auditEmailAddresses.isNotEmpty()) {
      throw RuntimeException("User with email address ${newUserDetails.emailAddress} already exists")
    }
    if (auditUsernames.isNotEmpty()) {
      throw RuntimeException("User with username ${newUserDetails.username} already exists")
    }
  }

  private fun saveRecords(newUserDetails: UserCreationEvent) {
    val savedAuditUser: AuditUser = auditUserRepository.save(AuditUser())

    auditUserIdRepository.save(AuditUserId(auditUser = savedAuditUser, userId = newUserDetails.userId))
    auditEmailAddressRepository.save(AuditEmailAddress(auditUser = savedAuditUser, emailAddress = newUserDetails.emailAddress))
    auditUsernameRepository.save(AuditUsername(auditUser = savedAuditUser, username = newUserDetails.username))
  }
}