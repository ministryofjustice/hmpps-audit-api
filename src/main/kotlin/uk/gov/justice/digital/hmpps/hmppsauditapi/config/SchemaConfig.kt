package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.apache.avro.Schema
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SchemaConfig {

  @Bean
  fun schema(): Schema = Schema.Parser().parse(javaClass.getResourceAsStream("/audit_event.avsc"))
}
