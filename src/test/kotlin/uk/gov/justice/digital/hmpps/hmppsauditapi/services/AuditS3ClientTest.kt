package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.then
import org.mockito.kotlin.whenever
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.ColumnInfo
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse
import software.amazon.awssdk.services.athena.model.QueryExecution
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.QueryExecutionState
import software.amazon.awssdk.services.athena.model.QueryExecutionState.SUCCEEDED
import software.amazon.awssdk.services.athena.model.QueryExecutionStatus
import software.amazon.awssdk.services.athena.model.ResultConfiguration
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.ResultSetMetadata
import software.amazon.awssdk.services.athena.model.Row
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.athena.model.StartQueryExecutionResponse
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesAuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
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

  @Nested
  inner class Save {
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

  @Nested
  inner class QueryEvents {

    val startQueryExecutionRequest: StartQueryExecutionRequest = StartQueryExecutionRequest.builder()
      .queryString("SELECT * FROM databaseName.audit_event WHERE `when` >= '-1000000000-01-01T00:00:00Z' AND `when` <= '-1000000000-01-01T00:00:30Z' AND who = 'someone' AND subjectId = 'subjectId' AND subjectType = 'subjectType';")
      .queryExecutionContext(QueryExecutionContext.builder().database("databaseName").build())
      .resultConfiguration(ResultConfiguration.builder().outputLocation("outputLocation").build())
      .workGroup("workGroupName")
      .build()

    @Test
    fun `queryEvents should return results from Athena`() {
      // Given
      val queryExecutionId = "test-query-id"
      val expectedAuditDto = AuditDto(
        id = UUID.randomUUID(),
        what = "READ_USER",
        `when` = Instant.parse("2024-02-14T12:34:56Z"),
        operationId = "op-123",
        subjectId = "sub-456",
        subjectType = "User",
        correlationId = "corr-789",
        who = "test-user",
        service = "auth-service",
        details = "some details",
      )
      val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId("test-query-id").build()
      val getQueryResultsRequest: GetQueryResultsRequest = GetQueryResultsRequest.builder().queryExecutionId("test-query-id").build()

      whenever(athenaClient.startQueryExecution(startQueryExecutionRequest)).thenReturn(
        StartQueryExecutionResponse.builder().queryExecutionId(queryExecutionId).build(),
      )

      whenever(athenaClient.getQueryExecution(getQueryExecutionRequest)).thenReturn(
        GetQueryExecutionResponse.builder()
          .queryExecution(QueryExecution.builder().status(QueryExecutionStatus.builder().state(SUCCEEDED).build()).build()).build(),
      )

      val resultSet = ResultSet.builder()
        .resultSetMetadata(
          ResultSetMetadata.builder()
            .columnInfo(
              columnInfo("id"),
              columnInfo("what"),
              columnInfo("when"),
              columnInfo("operationId"),
              columnInfo("subjectId"),
              columnInfo("subjectType"),
              columnInfo("correlationId"),
              columnInfo("who"),
              columnInfo("service"),
              columnInfo("details"),
            )
            .build(),
        )
        .rows(
          listOf(
            Row.builder().data(
              Datum.builder().varCharValue(expectedAuditDto.id.toString()).build(),
              Datum.builder().varCharValue(expectedAuditDto.what).build(),
              Datum.builder().varCharValue(expectedAuditDto.`when`.toString()).build(),
              Datum.builder().varCharValue(expectedAuditDto.operationId).build(),
              Datum.builder().varCharValue(expectedAuditDto.subjectId).build(),
              Datum.builder().varCharValue(expectedAuditDto.subjectType).build(),
              Datum.builder().varCharValue(expectedAuditDto.correlationId).build(),
              Datum.builder().varCharValue(expectedAuditDto.who).build(),
              Datum.builder().varCharValue(expectedAuditDto.service).build(),
              Datum.builder().varCharValue(expectedAuditDto.details).build(),
            ).build(),
          ),
        ).build()

      whenever(athenaClient.getQueryResults(getQueryResultsRequest)).thenReturn(
        GetQueryResultsResponse.builder()
          .resultSet(resultSet)
          .build(),
      )

      // When
      val results = auditS3Client.queryEvents(
        DigitalServicesAuditFilterDto(
          startDateTime = Instant.MIN,
          endDateTime = Instant.MIN.plusSeconds(30),
          who = "someone",
          subjectId = "subjectId",
          subjectType = "subjectType",
        ),
      )

      // Then
      assertThat(results).hasSize(1)
      assertThat(results[0]).isEqualTo(expectedAuditDto)
    }

    @Test
    fun `queryEvents should throw exception if query fails`() {
      // Given
      val getQueryExecutionRequest: GetQueryExecutionRequest = GetQueryExecutionRequest.builder().queryExecutionId("test-query-id").build()
      val queryExecutionId = "test-query-id"
      whenever(athenaClient.startQueryExecution(startQueryExecutionRequest)).thenReturn(StartQueryExecutionResponse.builder().queryExecutionId(queryExecutionId).build())
      whenever(athenaClient.getQueryExecution(getQueryExecutionRequest)).thenReturn(
        GetQueryExecutionResponse.builder().queryExecution(
          QueryExecution.builder().status(
            QueryExecutionStatus.builder()
              .state(QueryExecutionState.FAILED)
              .build(),
          ).build(),
        ).build(),
      )

      // Then
      assertThrows<RuntimeException> {
        auditS3Client.queryEvents(DigitalServicesAuditFilterDto())
      }
    }

    private fun columnInfo(name: String): ColumnInfo = ColumnInfo.builder().name(name).type("string").build()
  }
}
