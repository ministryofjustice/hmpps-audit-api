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
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.PrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrisonerAuditResourcePagingTest : IntegrationTest() {

  @Autowired
  private lateinit var prisonerAuditRepository: PrisonerAuditRepository

  val serviceRequestBody = JSONObject().put("service", "launchpad")

  val listOfAudits = listOf(
    PrisonerAuditEvent(
      what = "MINIMUM_FIELDS_EVENT",
      `when` = Instant.parse("2021-04-04T17:17:30Z"),
    ),
    PrisonerAuditEvent(
      what = "COURT_REGISTER_BUILDING_UPDATE",
      `when` = Instant.parse("2021-04-03T10:15:30Z"),
      operationId = "badea6d876c62e2f5264c94c7b50875e",
      subjectId = "da2ea6d876c62e2f5264c94c7b5086re",
      subjectType = "PERSON",
      correlationId = "correlationId1",
      who = "bobby.beans",
      service = "court-register",
      details = "{\"courtId\":\"AAAMH1\",\"buildingId\":936,\"building\":{\"id\":936,\"courtId\":\"AAAMH1\",\"buildingName\":\"Main Court Name Changed\"}}",
    ),
    PrisonerAuditEvent(
      what = "OFFENDER_DELETED",
      `when` = Instant.parse("2021-04-01T15:15:30Z"),
      operationId = "cadea6d876c62e2f5264c94c7b50875e",
      subjectId = "da2ea6d876c62e2f5264c94c7b5086re",
      subjectType = "PERSON",
      correlationId = "correlationId2",
      who = "bobby.beans",
      service = "launchpad",
      details = "{\"offenderId\": \"97\"}",
    ),
    PrisonerAuditEvent(
      what = "OFFENDER_DELETED",
      `when` = Instant.parse("2020-12-31T08:11:30Z"),
      operationId = "dadea6d876c62e2f5264c94c7b50875e",
      subjectId = "da2ea6d876c62e2f5264c94c7b5086re",
      subjectType = "PERSON",
      correlationId = "correlationId3",
      who = "freddy.frog",
      service = "launchpad",
      details = "{\"offenderId\": \"98\"}",
    ),
  )

  @BeforeAll
  fun `insert test audit events`() {
    listOfAudits.forEach {
      prisonerAuditRepository.save(it)
    }
  }

  @AfterAll
  fun `remove test audit events`() {
    prisonerAuditRepository.deleteAll()
  }

  @Test
  fun `find full page of audit events`() {
    webTestClient.post().uri("/audit/prisoner/paged?page=0&size=4&sort=who")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_AUDIT"), scopes = listOf("read")))
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
      AuditFilterDto(startDateTime = null, endDateTime = null, service = "launchpad", who = null, what = null),
    )
  }

  @Test
  fun `find full page of descending ordered audit events`() {
    webTestClient.post().uri("/audit/prisoner/paged?page=0&size=4&sort=who,desc")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_AUDIT"), scopes = listOf("read")))
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
      AuditFilterDto(startDateTime = null, endDateTime = null, service = "launchpad", who = null, what = null),
    )
  }

  @Test
  fun `find first page of audit events`() {
    webTestClient.post().uri("/audit/prisoner/paged?page=0&size=3&sort=who")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_AUDIT"), scopes = listOf("read")))
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
    webTestClient.post().uri("/audit/prisoner/paged?page=2&size=1&sort=who")
      .headers(setAuthorisation(roles = listOf("ROLE_PRISONER_AUDIT"), scopes = listOf("read")))
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
