package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.stereotype.Service
// import org.springframework.transaction.annotation.Transactional
// import uk.gov.justice.digital.hmpps.hmppsauditapi.exception.UserAlreadyExistsException
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditUserRepository
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuthEmailAddressRepository
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuthUserIdRepository
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuthUsernameRepository
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuditUser
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthEmailAddress
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUserId
// import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.AuthUsername
// //import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSUserListener.UserCreationEvent
//
@Service
class UserService(
//  private val auditUserRepository: AuditUserRepository,
//  private val authUserIdRepository: AuthUserIdRepository,
//  private val authEmailAddressRepository: AuthEmailAddressRepository,
//  private val authUsernameRepository: AuthUsernameRepository,
) {
//
// //  @Transactional
// //  fun saveNewUserDetails(newUserDetails: UserCreationEvent) {
// //    checkUserDoesNotExist(newUserDetails)
// //    saveRecords(newUserDetails)
// //  }
// //
// //  private fun checkUserDoesNotExist(newUserDetails: UserCreationEvent) {
// //    val authUserIds: List<AuthUserId> = authUserIdRepository.findAllByUserId(newUserDetails.userId)
// //    val authEmailAddresses: List<AuthEmailAddress> = authEmailAddressRepository.findAllByEmailAddress(newUserDetails.emailAddress)
// //    val authUsernames: List<AuthUsername> = authUsernameRepository.findAllByUsername(newUserDetails.username)
// //
// //    ensureNewUserHasNoExistingRecord(authUserIds, "User with user ID ${newUserDetails.userId} already exists")
// //    ensureNewUserHasNoExistingRecord(authEmailAddresses, "User with email address ${newUserDetails.emailAddress} already exists")
// //    ensureNewUserHasNoExistingRecord(authUsernames, "User with username ${newUserDetails.username} already exists")
// //  }
//
//  private fun <T> ensureNewUserHasNoExistingRecord(list: List<T>, message: String) {
//    if (list.isNotEmpty()) {
//      throw UserAlreadyExistsException(message)
//    }
//  }
//
// //  private fun saveRecords(newUserDetails: UserCreationEvent) {
// //    val savedAuditUser: AuditUser = auditUserRepository.save(AuditUser())
// //    authUserIdRepository.save(AuthUserId(auditUser = savedAuditUser, userId = newUserDetails.userId))
// //    authEmailAddressRepository.save(AuthEmailAddress(auditUser = savedAuditUser, emailAddress = newUserDetails.emailAddress))
// //    authUsernameRepository.save(AuthUsername(auditUser = savedAuditUser, username = newUserDetails.username))
// //  }
}
