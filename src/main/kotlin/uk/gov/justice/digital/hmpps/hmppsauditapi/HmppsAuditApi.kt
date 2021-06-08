package uk.gov.justice.digital.hmpps.hmppsauditapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication()
@ConfigurationPropertiesScan
class HmppsAuditApi

fun main(args: Array<String>) {
  runApplication<HmppsAuditApi>(*args)
}
