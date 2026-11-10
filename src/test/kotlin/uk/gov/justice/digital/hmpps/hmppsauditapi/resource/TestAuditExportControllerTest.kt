package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.io.LocalInputFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.StaffAuditEvent
import java.nio.file.Files
import java.util.UUID

@MockitoSpyBean(types = [S3Client::class])
class TestAuditExportControllerTest(
  @Autowired private val repository: StaffAuditRepository,
  @Autowired private val s3Client: S3Client,
) : IntegrationTest() {
  private val path = "/internal/integration-test/STAFF/parquet-batches"
  private val role = listOf("ROLE_AUDIT_INTEGRATION_TEST")

  @Nested
  inner class Security {
    @Test
    fun `both endpoints require authentication`() {
      webTestClient.post().uri(path).exchange().expectStatus().isUnauthorized
      webTestClient.get().uri("$path/${UUID.randomUUID()}").exchange().expectStatus().isUnauthorized
    }

    @Test
    fun `both endpoints require the integration test role`() {
      webTestClient.post().uri(path).headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange().expectStatus().isForbidden
      webTestClient.get().uri("$path/${UUID.randomUUID()}").headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange().expectStatus().isForbidden
    }
  }

  @Nested
  inner class Validation {
    @Test
    fun `unknown batch returns not found`() {
      webTestClient.get().uri("$path/${UUID.randomUUID()}").headers(setAuthorisation(roles = role))
        .exchange().expectStatus().isNotFound
    }

    @Test
    fun `malformed batch ID returns bad request`() {
      webTestClient.get().uri("$path/not-a-uuid").headers(setAuthorisation(roles = role))
        .exchange().expectStatus().isBadRequest
    }
  }

  @Nested
  inner class HappyPath {
    @Test
    fun `creates ten database events and downloads only that batch without S3 writes`() {
      clearInvocations(s3Client)
      val batchId = webTestClient.post().uri(path).headers(setAuthorisation(roles = role))
        .exchange().expectStatus().isOk.expectBody()
        .jsonPath("$.batchId").isNotEmpty
        .returnResult().responseBody!!
        .let { jacksonObjectMapper().readTree(it).get("batchId").asText() }
      val events = repository.findAll().filter { it.correlationId == batchId }
      val unrelated = repository.save(StaffAuditEvent(what = "REAL_EVENT", correlationId = batchId))
      val file = Files.createTempFile("audit-download-test-", ".parquet")
      try {
        assertThat(events).hasSize(10)
        assertThat(events.map { it.id }).doesNotContainNull().doesNotHaveDuplicates()
        assertThat(events.map { it.what }).containsOnly("INTEGRATION_TEST")
        assertThat(events.map { it.`when` }.distinct()).hasSize(1)
        assertThat(events.map { it.operationId }).containsOnly(batchId)
        assertThat(events.map { it.subjectId }).containsOnly("TEST_$batchId")
        assertThat(events.map { it.subjectType }).containsOnly("USER_ID")
        assertThat(events.map { it.correlationId }).containsOnly(batchId)
        assertThat(events.map { it.who }).containsOnly("TEST_$batchId")
        assertThat(events.map { it.service }).containsOnly("hmpps-audit-api-parquet-test")
        assertThat(events.map { it.details }).containsExactlyInAnyOrderElementsOf(
          (1..10).map { """{"eventNumber":$it}""" },
        )
        val bytes = webTestClient.get().uri("$path/$batchId").headers(setAuthorisation(roles = role))
          .exchange().expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
          .expectHeader().valueEquals(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-test-$batchId.parquet\"")
          .expectBody().returnResult().responseBody!!
        Files.write(file, bytes)
        AvroParquetReader.builder<GenericRecord>(LocalInputFile(file)).build().use { reader ->
          val records = generateSequence { reader.read() }.toList()
          assertThat(records).hasSize(10)
          assertThat(records.map { it.get("id").toString() }).containsExactlyInAnyOrderElementsOf(events.map { it.id.toString() })
          val eventsById = events.associateBy { it.id.toString() }
          records.forEach { record ->
            val event = eventsById.getValue(record.get("id").toString())
            assertThat(record.get("what").toString()).isEqualTo(event.what)
            assertThat(record.get("when").toString()).isEqualTo(event.`when`.toString())
            assertThat(record.get("operationId").toString()).isEqualTo(event.operationId)
            assertThat(record.get("subjectId").toString()).isEqualTo(event.subjectId)
            assertThat(record.get("subjectType").toString()).isEqualTo(event.subjectType)
            assertThat(record.get("correlationId").toString()).isEqualTo(event.correlationId)
            assertThat(record.get("who").toString()).isEqualTo(event.who)
            assertThat(record.get("service").toString()).isEqualTo(event.service)
            assertThat(record.get("details").toString()).isEqualTo(event.details)
          }
        }
        repository.delete(events.first())
        webTestClient.get().uri("$path/$batchId").headers(setAuthorisation(roles = role))
          .exchange().expectStatus().isEqualTo(409)
        verifyNoInteractions(s3Client)
      } finally {
        repository.deleteAll(events + unrelated)
        Files.deleteIfExists(file)
      }
    }
  }
}
