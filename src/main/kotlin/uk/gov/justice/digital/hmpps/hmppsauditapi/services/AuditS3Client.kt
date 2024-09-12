package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
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
      val parquetBytes = convertToParquetBytes(auditEvent, fileName)
      val md5Digest = MessageDigest.getInstance("MD5").digest(parquetBytes)
      val md5Base64 = Base64.getEncoder().encodeToString(md5Digest)

      val putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .contentMD5(md5Base64)
        .build()

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(parquetBytes))

      if (System.getProperty("spring.profiles.active").contains("dev")) {
        readParquetFileFromS3(fileName)
      }
    } catch (e: Exception) {
      telemetryClient.trackEvent("mohamad-test", mapOf(Pair("error", e.message ?: "unknown error")))

      val stringWriter = StringWriter()
      e.printStackTrace(PrintWriter(stringWriter))
      val stackTraceAsString = stringWriter.toString()
      telemetryClient.trackEvent("mohamad-test", mapOf("cause" to stackTraceAsString))
    }
  }

  private fun generateFilename(auditEvent: HMPPSAuditListener.AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.id}.parquet"
  }

  fun convertToParquetBytes(auditEvent: HMPPSAuditListener.AuditEvent, filename: String): ByteArray {
    val schema: Schema = Schema.Parser().parse(javaClass.getResourceAsStream("/audit_event.avsc"))
    val record: GenericRecord = GenericData.Record(schema).apply {
      put("id", auditEvent.id?.toString())
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

    val tempFilePath = Paths.get(System.getProperty("java.io.tmpdir"), "parquet-${auditEvent.id}.parquet")
    val outputPath = Path(tempFilePath.toUri())

    AvroParquetWriter.builder<GenericRecord>(outputPath)
      .withSchema(schema)
      .withCompressionCodec(CompressionCodecName.SNAPPY)
      .withConf(Configuration())
      .build().use { writer ->
        writer.write(record)
      }

    val parquetBytes = Files.readAllBytes(tempFilePath)
    Files.delete(tempFilePath)
    return parquetBytes
  }

  private fun readParquetFileFromS3(fileName: String) {
    val tempFile = File.createTempFile("parquet-temp", ".parquet")
    tempFile.deleteOnExit()
    val getObjectRequest = GetObjectRequest.builder()
      .bucket(bucketName)
      .key(fileName)
      .build()

    s3Client.getObject(getObjectRequest, Paths.get(tempFile.toURI()))
    val reader = AvroParquetReader.builder<GenericRecord>(Path(tempFile.toURI())).build()

    var record: GenericRecord?
    while (reader.read().also { record = it } != null) {
      telemetryClient.trackEvent("mohamad-test", mapOf("record" to record.toString()))
    }
    reader.close()
  }
}
