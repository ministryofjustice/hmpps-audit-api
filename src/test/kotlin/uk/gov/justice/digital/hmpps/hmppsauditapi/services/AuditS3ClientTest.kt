package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.then
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import java.nio.file.Files
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AuditS3ClientTest {

  @Mock
  private lateinit var s3Client: S3Client

  @Mock
  private lateinit var athenaClient: AthenaClient

  private lateinit var auditS3Client: AuditS3Client

  @Captor
  private lateinit var putObjectRequestCaptor: ArgumentCaptor<PutObjectRequest>

  @Captor
  private lateinit var requestBodyCaptor: ArgumentCaptor<RequestBody>

  private val auditEventId = UUID.randomUUID()

  private val schema: Schema = Schema.Parser().parse(javaClass.getResourceAsStream("/audit_event.avsc"))

  @BeforeEach
  fun setup() {
    auditS3Client = AuditS3Client(
      s3Client,
      schema,
      athenaClient,
      "databaseName",
      "workGroupName",
      "outputLocation",
      "bucketName",
    )
  }

  @Test
  fun `save should upload audit event to S3 with correct filename and content`() {
    val auditEvent = HMPPSAuditListener.AuditEvent(
      id = auditEventId,
      what = "some event",
      `when` = LocalDateTime.of(2020, 12, 31, 13, 30).toInstant(ZoneOffset.UTC),
      operationId = "some operation ID",
      subjectId = "some subject ID",
      subjectType = "some subject type",
      correlationId = "some correlation ID",
      who = "testUser",
      service = "some service",
      details = "some details",
    )

    auditS3Client.save(auditEvent)

    verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture())
    val putObjectRequest = putObjectRequestCaptor.value
    assertThat(putObjectRequest.bucket()).isEqualTo("bucketName")
    assertThat(putObjectRequest.key()).isEqualTo("year=2020/month=12/day=31/user=testUser/$auditEventId.parquet")
    verifyUploadedFileContents(auditEvent, putObjectRequest)
    val tempFilePath = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "${auditEvent.id}.parquet")
    assertThat(Files.exists(tempFilePath)).isFalse()
  }

  @Test
  fun `save should throw exception when ID is missing`() {
    val auditEvent = HMPPSAuditListener.AuditEvent(
      what = "some event",
      `when` = LocalDateTime.of(2020, 12, 31, 13, 30).toInstant(ZoneOffset.UTC),
      operationId = "some operation ID",
      subjectId = "some subject ID",
      subjectType = "some subject type",
      correlationId = "some correlation ID",
      who = "testUser",
      service = "some service",
      details = "some details",
    )

    val exception = assertThrows<IllegalArgumentException> { auditS3Client.save(auditEvent) }
    assertThat(exception.message).isEqualTo("ID cannot be null")
    then(s3Client).shouldHaveNoInteractions()
  }

  private fun verifyUploadedFileContents(
    auditEvent: HMPPSAuditListener.AuditEvent,
    putObjectRequest: PutObjectRequest,
  ) {
    val requestBody = requestBodyCaptor.value
    val parquetBytes = requestBody.contentStreamProvider().newStream().readAllBytes()

    val tempFile = Files.createTempFile("test-parquet", ".parquet").toFile()
    Files.write(tempFile.toPath(), parquetBytes)

    val reader = AvroParquetReader.builder<GenericRecord>(Path(tempFile.toURI())).build()

    val record: GenericRecord = reader.read()

    assertThat(record.get("id").toString()).isEqualTo(auditEvent.id.toString())
    assertThat(record.get("what").toString()).isEqualTo(auditEvent.what)
    assertThat(record.get("when").toString()).isEqualTo(auditEvent.`when`.toString())
    assertThat(record.get("operationId").toString()).isEqualTo(auditEvent.operationId)
    assertThat(record.get("subjectId").toString()).isEqualTo(auditEvent.subjectId)
    assertThat(record.get("subjectType").toString()).isEqualTo(auditEvent.subjectType)
    assertThat(record.get("correlationId").toString()).isEqualTo(auditEvent.correlationId)
    assertThat(record.get("who").toString()).isEqualTo(auditEvent.who)
    assertThat(record.get("service").toString()).isEqualTo(auditEvent.service)
    assertThat(record.get("details").toString()).isEqualTo(auditEvent.details)

    val expectedMd5 = MessageDigest.getInstance("MD5").digest(parquetBytes)
    val expectedMd5Base64 = Base64.getEncoder().encodeToString(expectedMd5)
    assertThat(putObjectRequest.contentMD5()).isEqualTo(expectedMd5Base64)
  }
}
