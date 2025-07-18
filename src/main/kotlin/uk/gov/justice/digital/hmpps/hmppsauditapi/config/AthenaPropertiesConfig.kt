package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AthenaPropertiesConfig(
  @Value("\${aws.athena.database}") private val databaseName: String,
  @Value("\${aws.athena.table}") private val tableName: String,
  @Value("\${aws.athena.workgroup}") private val workGroupName: String,
  @Value("\${aws.athena.outputLocation}") private val outputLocation: String,
  @Value("\${aws.athena.prisonerDatabase}") private val prisonerDatabaseName: String,
  @Value("\${aws.athena.prisonerTable}") private val prisonerTableName: String,
  @Value("\${aws.athena.prisonerWorkgroup}") private val prisonerWorkGroupName: String,
  @Value("\${aws.athena.prisonerOutputLocation}") private val prisonerOutputLocation: String,
) {

  @Bean
  @Qualifier("staffAthenaProperties")
  fun staffProperties(): AthenaProperties = AthenaProperties(
    databaseName = databaseName,
    tableName = tableName,
    workGroupName = workGroupName,
    outputLocation = outputLocation,
  )

  @Bean
  @Qualifier("prisonerAthenaProperties")
  fun prisonProperties(): AthenaProperties = AthenaProperties(
    databaseName = prisonerDatabaseName,
    tableName = prisonerTableName,
    workGroupName = prisonerWorkGroupName,
    outputLocation = prisonerOutputLocation,
  )
}

data class AthenaProperties(
  val databaseName: String,
  val tableName: String,
  val workGroupName: String,
  val outputLocation: String,
)
