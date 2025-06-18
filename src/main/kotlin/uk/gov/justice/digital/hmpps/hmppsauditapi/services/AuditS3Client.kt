package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.ZoneId
import java.util.Base64

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val schema: Schema,
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

  private fun generateFilename(auditEvent: HMPPSAuditListener.AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.id}.parquet"
  }

  private fun convertToParquetBytes(auditEvent: HMPPSAuditListener.AuditEvent): ByteArray {
    val tempFilePath = Paths.get(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
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

      val outputFile = LocalTempAuditFileOutput(tempFilePath)
      AvroParquetWriter.builder<GenericRecord>(outputFile)
        .withSchema(schema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withConf(Configuration())
        .build().use { writer ->
          writer.write(record)
        }
      return Files.readAllBytes(tempFilePath)
    } finally {
      Files.deleteIfExists(tempFilePath)
    }
  }
}
