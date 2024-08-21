package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.util.HadoopOutputFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneId
import java.util.Base64

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val telemetryClient: TelemetryClient,
  @Value("\${aws.s3.auditBucketName}") private val bucketName: String,

) {

  fun save(auditEvent: HMPPSAuditListener.AuditEvent) {
    try {
      val fileName = generateFilename(auditEvent)
      val parquetBytes = convertToParquet(auditEvent)
      val md5Base64 = generateContentMD5(parquetBytes)

      val putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .contentMD5(md5Base64)
        .build()

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(parquetBytes))

      val getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .build()

      val s3Object = s3Client.getObject(getObjectRequest)
      val objectContent = s3Object.readAllBytes()
      val objectAsString = String(objectContent, StandardCharsets.UTF_8)
      telemetryClient.trackEvent("mohamad-test", mapOf(Pair("parquet file", objectAsString)))
    } catch (e: Exception) {
      telemetryClient.trackEvent("mohamad-test", mapOf(Pair("error", e.message ?: "unknown error")))
    }
  }

  private fun convertToParquet(auditEvent: HMPPSAuditListener.AuditEvent): ByteArray {
    val schema = Schema.Parser().parse(javaClass.getResourceAsStream("/audit_event.avsc"))
    val record: GenericRecord = GenericData.Record(schema)
    record.put("operationId", auditEvent.operationId)
    record.put("what", auditEvent.what)
    record.put("when", auditEvent.`when`.toString())
    record.put("who", auditEvent.who)
    record.put("service", auditEvent.service)
    record.put("details", auditEvent.details)

    val byteArrayOutputStream = ByteArrayOutputStream()
    val outputFile = HadoopOutputFile.fromPath(Path("/tmp/audit_event.parquet"), Configuration())
    val writer: ParquetWriter<GenericRecord> = AvroParquetWriter.builder<GenericRecord>(outputFile)
      .withSchema(schema)
      .withCompressionCodec(CompressionCodecName.SNAPPY)
      .build()

    writer.write(record)
    writer.close()

    return byteArrayOutputStream.toByteArray()
  }

  private fun generateContentMD5(parquetBytes: ByteArray): String? {
    val md5Digest = MessageDigest.getInstance("MD5").digest(parquetBytes)
    return Base64.getEncoder().encodeToString(md5Digest)
  }

  private fun generateFilename(auditEvent: HMPPSAuditListener.AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.operationId}.parquet"
  }
}
