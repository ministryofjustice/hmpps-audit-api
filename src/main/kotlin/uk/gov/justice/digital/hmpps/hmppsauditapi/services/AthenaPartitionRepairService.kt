package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

@Component
class AthenaPartitionRepairService(
  private val s3Client: S3Client,
  private val athenaClient: AthenaClient,
  @Value("\${aws.s3.auditBucketName}") private val bucketName: String,
  @Value("\${aws.athena.database}") private val athenaDatabase: String,
  @Value("\${aws.athena.resultsLocation}") private val athenaResultsLocation: String,
) {

  @Scheduled(cron = "0 55 * * * *")
  fun repairPartitions() {
    val partitions = fetchPartitionsFromS3()
    if (partitions.isNotEmpty()) {
      val alterTableQuery = partitions
        .sortedWith(compareBy({ it.year }, { it.month }, { it.day }, { it.user }))
        .joinToString(separator = ",\n", prefix = "ALTER TABLE audit_dev_glue_catalog_database.audit_event ADD IF NOT EXISTS\n") { partition ->
          "PARTITION (year='${partition.year}', month='${partition.month}', day='${partition.day}', user='${partition.user}') " +
            "LOCATION 's3://$bucketName/year=${partition.year}/month=${partition.month}/day=${partition.day}/user=${partition.user}/'"
        }

      athenaClient.startQueryExecution(
        StartQueryExecutionRequest.builder()
          .queryString(alterTableQuery)
          .queryExecutionContext(QueryExecutionContext.builder().database(athenaDatabase).build())
          .resultConfiguration(ResultConfiguration.builder().outputLocation(athenaResultsLocation).build())
          .build(),
      )
    }
  }

  private fun fetchPartitionsFromS3(): Set<Partition> {
    val partitions = mutableSetOf<Partition>()
    var continuationToken: String? = null

    do {
      val response = s3Client.listObjectsV2(
        ListObjectsV2Request.builder()
          .bucket(bucketName)
          .prefix("year=")
          .continuationToken(continuationToken)
          .build(),
      )

      response.contents().asSequence()
        .mapNotNull { PARTITION_REGEX.find(it.key())?.destructured }
        .map { (year, month, day, user) -> Partition(year, month, day, user) }
        .forEach { partitions.add(it) }

      continuationToken = response.nextContinuationToken()
    } while (continuationToken != null)

    return partitions
  }

  data class Partition(val year: String, val month: String, val day: String, val user: String)

  companion object {
    val PARTITION_REGEX = Regex("""year=(\d+)/month=(\d+)/day=(\d+)/user=([^/]+)/""")
  }
}
