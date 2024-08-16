package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.springframework.test.context.ActiveProfilesResolver

/**
 * If the S3 bucket tests are being executed locally then we want the test profile to be active.
 * If it's being tested in CircleCI then we want to the active profiles to be taken in from the command line
 */
class CommandLineProfilesResolver : ActiveProfilesResolver {
  override fun resolve(testClass: Class<*>): Array<String> {
    val cmdProfiles = System.getProperty("spring.profiles.active")
      ?.split(",")
      ?.filter { it.isNotBlank() }
      ?: listOf("test")

    return if (cmdProfiles.isEmpty()) arrayOf("test") else cmdProfiles.toTypedArray()
  }
}
