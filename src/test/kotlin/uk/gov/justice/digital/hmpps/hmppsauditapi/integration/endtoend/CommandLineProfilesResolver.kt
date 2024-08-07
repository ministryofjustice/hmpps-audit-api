package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ActiveProfilesResolver

class CommandLineProfilesResolver : ActiveProfilesResolver {
  override fun resolve(testClass: Class<*>): Array<String> {
    val cmdProfiles = System.getProperty("spring.profiles.active")?.split(",") ?: emptyList()

    if (cmdProfiles.isNotEmpty()) {
      return cmdProfiles.toTypedArray()
    }

    return testClass.getAnnotation(ActiveProfiles::class.java)?.profiles ?: emptyArray()
  }
}
