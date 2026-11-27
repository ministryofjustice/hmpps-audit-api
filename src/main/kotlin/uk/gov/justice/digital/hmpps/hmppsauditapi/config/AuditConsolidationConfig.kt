package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Configuration
class AuditConsolidationConfig(
  @Value("\${hmpps.audit.consolidation.enabled:false}") val featureFlagEnabled: Boolean,
  @Value("\${hmpps.audit.consolidation.enabled-types:STAFF}") val enabledAuditEventTypes: Set<AuditEventType>,
  athenaPropertiesFactory: AthenaPropertiesFactory,
) {
  init {
    if (featureFlagEnabled) {
      enabledAuditEventTypes.forEach {
        require(athenaPropertiesFactory.getProperties(it).s3BucketName.isNotBlank()) {
          "Audit consolidations for $it require an audit bucket to be configured"
        }
      }
    }
  }

  @Bean(defaultCandidate = false)
  fun auditConsolidationExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
    corePoolSize = 1
    maxPoolSize = 1
    queueCapacity = 1
    setThreadNamePrefix("audit-consolidation-")
  }
}
