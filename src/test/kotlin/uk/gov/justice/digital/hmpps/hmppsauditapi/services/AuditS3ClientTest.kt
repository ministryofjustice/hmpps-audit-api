package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
@Disabled
class AuditS3ClientTest {

  @Mock
  private lateinit var s3Client: S3Client

  @Mock
  private lateinit var objectMapper: ObjectMapper

  @Mock
  private lateinit var telemetryClient: TelemetryClient

  private lateinit var auditS3Client: AuditS3Client

  @Captor
  private lateinit var putObjectRequestCaptor: ArgumentCaptor<PutObjectRequest>

  @Captor
  private lateinit var requestBodyCaptor: ArgumentCaptor<RequestBody>

  @BeforeEach
  fun setup() {
    auditS3Client = AuditS3Client(s3Client, telemetryClient, "bucketName")
  }

  @Test
  fun `save should upload audit event to S3 with correct filename and content`() {
    val auditEvent = HMPPSAuditListener.AuditEvent(
      `when` = LocalDateTime.of(2020, 12, 31, 13, 30).toInstant(ZoneOffset.UTC),
      what = "testEvent",
      who = "testUser",
    )
    val auditEventJsonString = """{"when":"${auditEvent.`when`}","what":"testEvent","who":"testUser","details":{"key":"value"}}"""
    `when`(objectMapper.writeValueAsString(auditEvent)).thenReturn(auditEventJsonString)

    auditS3Client.save(auditEvent)

    verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture())
    val putObjectRequest = putObjectRequestCaptor.value
    assertThat(putObjectRequest.bucket()).isEqualTo("bucketName")
    assertThat(putObjectRequest.key()).isEqualTo("year=2020/month=12/day=31/user=testUser/07c867b1fb88d84fdcb16090619da3da.json")
    assertThat(String(requestBodyCaptor.value.contentStreamProvider().newStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(auditEventJsonString)
  }
}
