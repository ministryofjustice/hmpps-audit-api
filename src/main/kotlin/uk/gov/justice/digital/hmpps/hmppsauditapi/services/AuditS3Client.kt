package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesAuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import java.util.UUID

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val schema: Schema,
  private val athenaClient: AthenaClient,
  @Value("\${aws.athena.database}") private val databaseName: String,
  @Value("\${aws.athena.workgroup}") private val workGroup: String,
  @Value("\${aws.athena.outputLocation}") private val outputLocation: String,
  @Value("\${aws.s3.auditBucketName}") private val bucketName: String,
) {

  fun save(auditEvent: HMPPSAuditListener.AuditEvent) {
    val fileName = generateFilename(auditEvent)
    val parquetBytes = convertToParquetBytes(auditEvent)
    val md5Digest = MessageDigest.getInstance("MD5").digest(parquetBytes)
    val md5Base64 = Base64.getEncoder().encodeToString(md5Digest)

    val putObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(fileName)
      .contentMD5(md5Base64)
      .build()

    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(parquetBytes))
  }

  fun queryEvents(filter: DigitalServicesAuditFilterDto): List<AuditDto> {
    val query = buildAthenaQuery(filter)

    val queryExecutionId = startAthenaQuery(query)
    waitForQueryCompletion(queryExecutionId)

    return fetchQueryResults(queryExecutionId)
  }

  private fun generateFilename(auditEvent: HMPPSAuditListener.AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.id}.parquet"
  }

  private fun convertToParquetBytes(auditEvent: HMPPSAuditListener.AuditEvent): ByteArray {
    val tempFileJavaPath = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
    try {
      val record: GenericRecord = GenericData.Record(schema).apply {
        put("id", auditEvent.id?.toString() ?: throw IllegalArgumentException("ID cannot be null"))
        put("what", auditEvent.what)
        put("when", auditEvent.`when`.toString())
        put("operationId", auditEvent.operationId)
        put("subjectId", auditEvent.subjectId)
        put("subjectType", auditEvent.subjectType)
        put("correlationId", auditEvent.correlationId)
        put("who", auditEvent.who)
        put("service", auditEvent.service)
        put("details", auditEvent.details)
      }

      val tempFilePath = Path(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
      AvroParquetWriter.builder<GenericRecord>(tempFilePath)
        .withSchema(schema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withConf(Configuration())
        .build().use { writer ->
          writer.write(record)
        }
      return Files.readAllBytes(tempFileJavaPath)
    } finally {
      Files.deleteIfExists(tempFileJavaPath)
    }
  }

  private fun buildAthenaQuery(filter: DigitalServicesAuditFilterDto): String {
    val conditions = mutableListOf<String>()

    filter.startDateTime?.let { conditions.add("`when` >= '$it'") }
    filter.endDateTime?.let { conditions.add("`when` <= '$it'") }
    filter.who?.let { conditions.add("who = '$it'") }
    filter.subjectId?.let { conditions.add("subjectId = '$it'") }
    filter.subjectType?.let { conditions.add("subjectType = '$it'") }

    val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""

    return "SELECT * FROM $databaseName.audit_event $whereClause;"
  }

  private fun startAthenaQuery(query: String): String {
    val request = StartQueryExecutionRequest.builder()
      .queryString(query)
      .queryExecutionContext { it.database(databaseName) }
      .workGroup(workGroup)
      .resultConfiguration { it.outputLocation(outputLocation) }
      .build()

    val response = athenaClient.startQueryExecution(request)
    return response.queryExecutionId()
  }

  private fun waitForQueryCompletion(queryExecutionId: String) {
    while (true) {
      val status = athenaClient.getQueryExecution(
        GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build(),
      ).queryExecution().status().state()

      when (status) {
        QueryExecutionState.SUCCEEDED -> return
        QueryExecutionState.FAILED, QueryExecutionState.CANCELLED ->
          throw RuntimeException("Query failed or was cancelled: $queryExecutionId")
        else -> Thread.sleep(5000) // Wait before retrying
      }
    }
  }

  private fun fetchQueryResults(queryExecutionId: String): List<AuditDto> {
    val request = GetQueryResultsRequest.builder()
      .queryExecutionId(queryExecutionId)
      .build()

    val results = mutableListOf<AuditDto>()
    var nextToken: String? = null

    do {
      val response = athenaClient.getQueryResults(
        request.toBuilder().nextToken(nextToken).build(),
      )

      val columnNames = response.resultSet().resultSetMetadata().columnInfo().map { it.name() }

      response.resultSet().rows().drop(1).forEach { row ->
        val values = row.data().map { it.varCharValue() ?: "" }
        val resultMap = columnNames.zip(values).toMap()

        val auditDto = AuditDto(
          id = UUID.fromString(resultMap["id"] ?: throw IllegalArgumentException("Missing ID")),
          what = resultMap["what"] ?: "",
          `when` = Instant.parse(resultMap["when"] ?: throw IllegalArgumentException("Missing timestamp")),
          operationId = resultMap["operationId"],
          subjectId = resultMap["subjectId"],
          subjectType = resultMap["subjectType"],
          correlationId = resultMap["correlationId"],
          who = resultMap["who"],
          service = resultMap["service"],
          details = resultMap["details"],
        )

        results.add(auditDto)
      }

      nextToken = response.nextToken()
    } while (nextToken != null)

    return results
  }
}
