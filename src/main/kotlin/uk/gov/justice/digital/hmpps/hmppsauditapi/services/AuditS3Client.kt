package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneId
import java.util.Base64

@Service
class AuditS3Client(
  private val s3Client: S3Client,
  private val objectMapper: ObjectMapper,
  private val telemetryClient: TelemetryClient,
  @Value("\${aws.s3.auditBucketName}") private val bucketName: String,
) {

  fun save(auditEvent: HMPPSAuditListener.AuditEvent) {
    val auditEventJsonString = objectMapper.writeValueAsString(auditEvent)
    val fileName = generateFilename(auditEvent, auditEventJsonString)

    val jsonBytes = auditEventJsonString.toByteArray(StandardCharsets.UTF_8)

    val md5Digest = MessageDigest.getInstance("MD5").digest(jsonBytes)
    val md5Base64 = Base64.getEncoder().encodeToString(md5Digest)

    val putObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(fileName)
      .contentMD5(md5Base64)
      .build()

    try {
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(jsonBytes))
      val getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(fileName)
        .build()

      val s3Object = s3Client.getObject(getObjectRequest)
      val objectContent = s3Object.readAllBytes()
      val jsonString = objectContent.toString(StandardCharsets.UTF_8)
      telemetryClient.trackEvent("hmpps-audit-mohamad", mapOf("json file" to jsonString))
    } catch (e: Exception) {
      telemetryClient.trackEvent("hmpps-audit-mohamad", mapOf("errorMessage" to (e.message ?: "Unknown error")))
    }
  }

  private fun generateFilename(auditEvent: HMPPSAuditListener.AuditEvent, auditEventJsonString: String): String {
    val whenDateTime = auditEvent.`when`.atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "year=${whenDateTime.year}/month=${whenDateTime.monthValue}/day=${whenDateTime.dayOfMonth}/user=${auditEvent.who}/" +
      "${getMd5HashFromJsonString(auditEventJsonString)}.json"
  }

  private fun getMd5HashFromJsonString(jsonString: String): String {
    val bytes = jsonString.toByteArray()
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
  }
}
