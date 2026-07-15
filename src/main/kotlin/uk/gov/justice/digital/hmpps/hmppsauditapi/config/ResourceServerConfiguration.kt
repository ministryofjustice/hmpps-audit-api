package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration {
  @Bean
  fun resourceServerConfigurationCustomizer(): ResourceServerConfigurationCustomizer = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/ping",
        "/queue-admin/retry-all-dlqs",
      )
    }
  }
}
