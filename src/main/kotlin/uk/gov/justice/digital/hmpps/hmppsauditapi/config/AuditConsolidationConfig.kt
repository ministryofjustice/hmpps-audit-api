package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Configuration
class AuditConsolidationConfig(
  @Value("\${hmpps.audit.consolidation.enabled:false}") val enabled: Boolean,
  @Value("\${hmpps.audit.consolidation.bucket-name:}") val bucketName: String,
  @Value("\${hmpps.audit.consolidation.enabled-types:STAFF}") val enabledTypes: Set<AuditEventType>,
  athenaPropertiesFactory: AthenaPropertiesFactory,
) {
  init {
    if (enabled) {
      require(bucketName.isNotBlank()) { "An audit consolidation bucket must be configured when consolidations are enabled" }
      require(AuditEventType.entries.none { athenaPropertiesFactory.getProperties(it).s3BucketName == bucketName }) {
        "Audit consolidations must use a separate bucket from existing audit ingestion"
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
