package uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.QueueListenerIntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditS3Client
import java.time.Instant
import java.util.UUID

class AuditTest @Autowired constructor(
  override var webTestClient: WebTestClient,
) : QueueListenerIntegrationTest() {

  private val basicAuditEvent = AuditEvent(what = "basicAuditEvent")

  @SpyBean
  private lateinit var s3Client: S3Client

  @Value("\${aws.s3.auditBucketName}")
  private lateinit var bucketName: String

  @SpyBean
  private lateinit var auditS3Client: AuditS3Client

  @BeforeEach
  fun setup() {
    try {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
      Thread.sleep(2000)
      println("hello")
    } catch (exception: S3Exception) {
      if (exception.awsErrorDetails().errorCode() != null) {
        throw exception
      }
    }
  }

  @Test
  fun `will consume an audit event message`() {
    System.setProperty("hmpps.repository.saveToS3Bucket", "false")
    val message = """
    {
      "what": "OFFENDER_DELETED",
      "when": "2021-01-25T12:30:00Z",
      "operationId": "badea6d876c62e2f5264c94c7b50875e",
      "subjectId": "y1dea6d876c62e2f5264c94c7b50875r",
      "subjectType": "PERSON",
      "who": "bobby.beans",
      "service": "offender-service",
      "details": "{ \"offenderId\": \"99\"}"
    }
    """

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(awsSqsUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    verify(telemetryClient).trackEvent(
      eq("hmpps-audit"),
      check {
        assertThat(it["what"]).isEqualTo("OFFENDER_DELETED")
        assertThat(it["when"]).isEqualTo("2021-01-25T12:30:00Z")
        assertThat(it["operationId"]).isEqualTo("badea6d876c62e2f5264c94c7b50875e")
        assertThat(it["who"]).isEqualTo("bobby.beans")
        assertThat(it["service"]).isEqualTo("offender-service")
        assertThat(it.containsKey("details")).isEqualTo(false)
      },
      isNull(),
    )
    verify(auditRepository).save(any<AuditEvent>())
  }

  @Test
  fun `save basic audit entry to database`() {
    System.setProperty("hmpps.repository.saveToS3Bucket", "false")
    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(basicAuditEvent))
      .exchange()
      .expectStatus().isAccepted

    await untilCallTo { mockingDetails(auditRepository).invocations.size } matches { it == 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(auditRepository).save(any<AuditEvent>())
    verifyNoInteractions(auditS3Client)
  }

  @Test
  fun `save basic audit entry to S3 bucket`() {
    System.setProperty("hmpps.repository.saveToS3Bucket", "true")
    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(basicAuditEvent))
      .exchange()
      .expectStatus().isAccepted

    await untilCallTo { mockingDetails(auditS3Client).invocations.size } matches { it == 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(auditS3Client).save(any<AuditEvent>())
    verifyNoInteractions(auditRepository)
  }

  @Test
  fun `save full audit entry to database`() {
    System.setProperty("hmpps.repository.saveToS3Bucket", "false")
    val auditEvent = AuditEvent(
      UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
      "OFFENDER_DELETED",
      Instant.parse("2021-04-01T15:15:30Z"),
      "cadea6d876c62e2f5264c94c7b50875e",
      "bobby.beans",
      "offender-service",
      "{\"offenderId\": \"97\"}",
    )

    webTestClient.post()
      .uri("/audit")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(auditEvent))
      .exchange()
      .expectStatus().isAccepted

    await untilCallTo { mockingDetails(auditRepository).invocations.size } matches { it == 1 }

    verify(telemetryClient).trackEvent(eq("hmpps-audit"), any(), isNull())
    verify(auditRepository).save(auditEvent)
    verifyNoInteractions(auditS3Client)
  }
}
