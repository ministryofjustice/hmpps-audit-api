package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
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
class AuditResourcePagingTest : IntegrationTest() {

  @Autowired
  private lateinit var auditRepository: AuditRepository

  val serviceRequestBody: JSONObject = JSONObject().put("service", "offender-service")
  val whatRequestBody: JSONObject = JSONObject().put("what", "OFFENDER_DELETED")
  val whoRequestBody: JSONObject = JSONObject().put("who", "bobby.beans")
  val whoAndWhatRequestBody: JSONObject = JSONObject().put("who", "bobby.beans").put("what", "OFFENDER_DELETED")

  val listOfAudits = listOf(
    AuditEvent(
      UUID.fromString("64505f1e-c9ca-4e54-8c62-d946359b667f"),
      "MINIMUM_FIELDS_EVENT",
      Instant.parse("2021-04-04T17:17:30Z")
    ),
    AuditEvent(
      UUID.fromString("5c5ba3d7-0707-42f1-b9ea-949e22dc17ba"),
      "COURT_REGISTER_BUILDING_UPDATE",
      Instant.parse("2021-04-03T10:15:30Z"),
      "badea6d876c62e2f5264c94c7b50875e",
      "bobby.beans",
      "court-register",
      "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}"
    ),
    AuditEvent(
      UUID.fromString("e5b4800c-dc4e-45f8-826c-877b1f3ce8de"),
      "OFFENDER_DELETED",
      Instant.parse("2021-04-01T15:15:30Z"),
      "cadea6d876c62e2f5264c94c7b50875e",
      "bobby.beans",
      "offender-service",
      "{\"offenderId\": \"97\"}"
    ),
    AuditEvent(
      UUID.fromString("03a1624a-54e7-453e-8c79-816dbe02fd3c"),
      "OFFENDER_DELETED",
      Instant.parse("2020-12-31T08:11:30Z"),
      "dadea6d876c62e2f5264c94c7b50875e",
      "freddy.frog",
      "offender-service",
      "{\"offenderId\": \"98\"}"
    )
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

  @Suppress("ClassName")
  @Nested
  inner class filteredAuditEvents {

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
        .jsonPath("$.content[0].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875e")
        .jsonPath("$.content[1].operationId").isEqualTo("dadea6d876c62e2f5264c94c7b50875e")
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
        .jsonPath("$.content[0].operationId").isEqualTo("badea6d876c62e2f5264c94c7b50875e")
        .jsonPath("$.content[1].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875e")
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
        .jsonPath("$.content[0].operationId").isEqualTo("cadea6d876c62e2f5264c94c7b50875e")
    }
  }
}
