package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneId

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

    val putObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(fileName)
      .build()

    val jsonBytes = auditEventJsonString.toByteArray(StandardCharsets.UTF_8)

    try {
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(jsonBytes))
      telemetryClient.trackEvent("hmpps-audit-mohamad", mapOf("success" to "successful" ))
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
