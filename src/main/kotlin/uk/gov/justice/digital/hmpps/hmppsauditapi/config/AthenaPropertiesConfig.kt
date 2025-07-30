package uk.gov.justice.digital.hmpps.hmppsauditapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType

@Configuration
class AthenaPropertiesConfig(
  @Value("\${aws.s3.auditBucketName}") private val auditBucketName: String,
  @Value("\${aws.s3.prisonerAuditBucketName}") private val prisonerAuditBucketName: String,
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
    auditEventType = AuditEventType.STAFF,
    s3BucketName = auditBucketName,
    databaseName = databaseName,
    tableName = tableName,
    workGroupName = workGroupName,
    outputLocation = outputLocation,
  )

  @Bean
  @Qualifier("prisonerAthenaProperties")
  fun prisonProperties(): AthenaProperties = AthenaProperties(
    auditEventType = AuditEventType.PRISONER,
    s3BucketName = prisonerAuditBucketName,
    databaseName = prisonerDatabaseName,
    tableName = prisonerTableName,
    workGroupName = prisonerWorkGroupName,
    outputLocation = prisonerOutputLocation,
  )
}

data class AthenaProperties(
  val auditEventType: AuditEventType,
  val s3BucketName: String,
  val databaseName: String,
  val tableName: String,
  val workGroupName: String,
  val outputLocation: String,
)
