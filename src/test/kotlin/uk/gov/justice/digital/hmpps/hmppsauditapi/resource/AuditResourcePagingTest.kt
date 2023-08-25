package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditResourcePagingTest : IntegrationTest() {

  @Autowired
  private lateinit var auditRepository: AuditRepository

  val serviceRequestBody = JSONObject().put("service", "offender-service")

  val listOfAudits = listOf(
    AuditEvent(
      UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
      "MINIMUM_FIELDS_EVENT",
      Instant.parse("2021-04-04T17:17:30Z"),
    ),
    AuditEvent(
      UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
      "COURT_REGISTER_BUILDING_UPDATE",
      Instant.parse("2021-04-03T10:15:30Z"),
      "badea6d876c62e2f5264c94c7b50875e",
      "da2ea6d876c62e2f5264c94c7b5086re",
      "PERSON",
      "bobby.beans",
      "court-register",
      "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
    ),
    AuditEvent(
      UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
      "OFFENDER_DELETED",
      Instant.parse("2021-04-01T15:15:30Z"),
      "cadea6d876c62e2f5264c94c7b50875e",
      "da2ea6d876c62e2f5264c94c7b5086re",
      "PERSON",
      "bobby.beans",
      "offender-service",
      "{\"offenderId\": \"97\"}",
    ),
    AuditEvent(
      UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
      "OFFENDER_DELETED",
      Instant.parse("2020-12-31T08:11:30Z"),
      "dadea6d876c62e2f5264c94c7b50875e",
      "da2ea6d876c62e2f5264c94c7b5086re",
      "PERSON",
      "freddy.frog",
      "offender-service",
      "{\"offenderId\": \"98\"}",
    ),
  )

  @BeforeAll
  fun `insert test audit events`() {
    listOfAudits.forEach {
      auditRepository.save(it)
    }
  }

  @AfterAll
  fun `remove test audit events`() {
    auditRepository.deleteAll()
  }

  @Test
  fun `find full page of audit events`() {
    webTestClient.post().uri("/audit/paged?page=0&size=4&sort=who")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(serviceRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(4)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[1].who").isEqualTo("freddy.frog")

    verify(auditQueueService).sendAuditAuditEvent(
      "AUDIT_GET_ALL_PAGED",
      AuditFilterDto(startDateTime = null, endDateTime = null, service = "offender-service", who = null, what = null),
    )
  }

  @Test
  fun `find full page of descending ordered audit events`() {
    webTestClient.post().uri("/audit/paged?page=0&size=4&sort=who,desc")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(serviceRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(4)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].who").isEqualTo("freddy.frog")
      .jsonPath("$.content[1].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[2].who").doesNotExist()

    verify(auditQueueService).sendAuditAuditEvent(
      "AUDIT_GET_ALL_PAGED",
      AuditFilterDto(startDateTime = null, endDateTime = null, service = "offender-service", who = null, what = null),
    )
  }

  @Test
  fun `find first page of audit events`() {
    webTestClient.post().uri("/audit/paged?page=0&size=3&sort=who")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(serviceRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[1].who").isEqualTo("freddy.frog")
  }

  @Test
  fun `find second page of audit events`() {
    webTestClient.post().uri("/audit/paged?page=2&size=1&sort=who")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(serviceRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(0)
      .jsonPath("$.size").isEqualTo(1)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(2)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].who").doesNotExist()
  }
}
