package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.ZoneId
import java.util.Base64

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val auditParquetWriter: AuditParquetWriter,
) {

  fun save(auditEvent: AuditEvent, bucketName: String) {
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

  fun saveConsolidatedAuditEvents(file: Path, bucketName: String, filename: String) {
    val md5Digest = MessageDigest.getInstance("MD5")
    DigestInputStream(Files.newInputStream(file), md5Digest).use {
      it.transferTo(OutputStream.nullOutputStream())
    }
    val request = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(filename)
      .contentMD5(Base64.getEncoder().encodeToString(md5Digest.digest()))
      .build()
    s3Client.putObject(request, RequestBody.fromFile(file))
  }

  fun download(bucketName: String, key: String, file: Path): Boolean = try {
    Files.deleteIfExists(file)
    s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build(), file)
    true
  } catch (_: NoSuchKeyException) {
    false
  }

  fun delete(bucketName: String, key: String) {
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build())
  }

  private fun generateFilename(auditEvent: AuditEvent): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${auditEvent.id}.parquet"
  }

  private fun convertToParquetBytes(auditEvent: AuditEvent): ByteArray {
    val tempFile = File(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
    try {
      auditParquetWriter.write(sequenceOf(auditEvent), tempFile.toPath())
      return Files.readAllBytes(tempFile.toPath())
    } finally {
      Files.deleteIfExists(tempFile.toPath())
    }
  }
}
