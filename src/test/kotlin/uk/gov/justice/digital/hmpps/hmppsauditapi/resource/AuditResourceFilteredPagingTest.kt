package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditResourceFilteredPagingTest : IntegrationTest() {

  @Autowired
  private lateinit var auditRepository: AuditRepository

  val whatRequestBody = JSONObject().put("what", "OFFENDER_DELETED")
  val whoRequestBody = JSONObject().put("who", "bobby.beans")
  val whoAndWhatRequestBody = JSONObject().put("who", "bobby.beans").put("what", "OFFENDER_DELETED")
  val startDateRequestBody = JSONObject().put("startDateTime", Instant.parse("2021-01-01T15:15:30Z")).put("endDateTime", Instant.parse("2021-12-31T17:17:30Z"))
  val endDateRequestBody = JSONObject().put("endDateTime", Instant.parse("2021-04-12T17:17:30Z"))
  val dateRangeRequestBody = JSONObject().put("startDateTime", Instant.parse("2021-01-01T15:15:30Z"))
    .put("endDateTime", Instant.parse("2021-04-12T17:17:30Z"))

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
      "badea6d876c62e2f5264c94c7b50875b",
      "da8ea6d876c62e2f5264c94c7b50867r",
      "PERSON",
      "bobby.beans",
      "court-register",
      "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
    ),
    AuditEvent(
      UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
      "OFFENDER_DELETED",
      Instant.parse("2021-04-01T15:15:30Z"),
      "cadea6d876c62e2f5264c94c7b50875c",
      "da4ea6d876c62e2f5264c94c7b508c57",
      "PERSON",
      "bobby.beans",
      "offender-service",
      "{\"offenderId\": \"97\"}",
    ),
    AuditEvent(
      UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
      "OFFENDER_DELETED",
      Instant.parse("2020-12-31T08:11:30Z"),
      "dadea6d876c62e2f5264c94c7b50875d",
      "mu2ea6d876c62e2f5264c94c7b508d57",
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
  fun `filter audit events by what`() {
    webTestClient.post().uri("/audit/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(whatRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].what").isEqualTo("OFFENDER_DELETED")
      .jsonPath("$.content[0].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875c")
      .jsonPath("$.content[1].what").isEqualTo("OFFENDER_DELETED")
      .jsonPath("$.content[1].operationId").isEqualTo("dadea6d876c62e2f5264c94c7b50875d")
  }

  @Test
  fun `filter audit events by who`() {
    webTestClient.post().uri("/audit/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(whoRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(2)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[0].operationId").isEqualTo("badea6d876c62e2f5264c94c7b50875b")
      .jsonPath("$.content[1].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[1].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875c")
  }

  @Test
  fun `filter audit events by what and who`() {
    webTestClient.post().uri("/audit/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(whoAndWhatRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(1)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(1)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].what").isEqualTo("OFFENDER_DELETED")
      .jsonPath("$.content[0].who").isEqualTo("bobby.beans")
      .jsonPath("$.content[0].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875c")
  }

  @Test
  fun `filter audit events by startDateTme`() {
    webTestClient.post().uri("/audit/paged?page=0&size=2")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("read")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(startDateRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(2)
      .jsonPath("$.size").isEqualTo(2)
      .jsonPath("$.totalElements").isEqualTo(3)
      .jsonPath("$.totalPages").isEqualTo(2)
      .jsonPath("$.last").isEqualTo(false)
      .jsonPath("$.content[0].operationId").doesNotExist()
  }

  @Test
  fun `filter audit events by endDateTime`() {
    webTestClient.post().uri("/audit/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(endDateRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(4)
      .jsonPath("$.totalPages").isEqualTo(2)
      .jsonPath("$.last").isEqualTo(false)
      .jsonPath("$.content[0].operationId").doesNotExist()
  }

  @Test
  fun `filter audit events by date range`() {
    webTestClient.post().uri("/audit/paged?page=0&size=3")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT"), scopes = listOf("write")))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(dateRangeRequestBody.toString())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.length()").isEqualTo(3)
      .jsonPath("$.size").isEqualTo(3)
      .jsonPath("$.totalElements").isEqualTo(3)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.last").isEqualTo(true)
      .jsonPath("$.content[0].operationId").doesNotExist()
  }
}
