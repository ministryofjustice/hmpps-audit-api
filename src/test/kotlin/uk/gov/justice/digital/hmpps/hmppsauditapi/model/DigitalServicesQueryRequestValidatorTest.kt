package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsauditapi.integration.S3TestConfig
import uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend.CommandLineProfilesResolver
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType.STAFF
import java.time.LocalDate
import java.util.stream.Stream

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTest.SqsConfig::class, JwtAuthHelper::class, S3TestConfig::class)
@ActiveProfiles(resolver = CommandLineProfilesResolver::class)
@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
class DigitalServicesQueryRequestValidatorTest {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class AuditFilterCases {

    @ParameterizedTest
    @MethodSource("validBaseAuditFilterDto")
    internal fun `should be valid`(digitalServicesQueryRequest: DigitalServicesQueryRequest) {
      val violations = validator.validate(digitalServicesQueryRequest)
      assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidBaseAuditFilterDto")
    internal fun `should be invalid`(digitalServicesQueryRequest: DigitalServicesQueryRequest, expectedErrors: Map<String, String>) {
      val actualErrors: Map<String, String> = validator.validate(digitalServicesQueryRequest).associate { it.propertyPath.toString() to it.message }
      assertThat(actualErrors).containsExactlyInAnyOrderEntriesOf(expectedErrors)
    }

    private fun validBaseAuditFilterDto() = listOf(
      DigitalServicesQueryRequest(
        auditEventType = STAFF,
        startDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now(),
        subjectId = "test-subject",
        subjectType = "USER_ID",
      ),
      DigitalServicesQueryRequest(
        auditEventType = STAFF,
        startDate = LocalDate.now(),
        subjectId = "test-subject",
        subjectType = "USER_ID",
      ),
      DigitalServicesQueryRequest(
        auditEventType = STAFF,
        endDate = LocalDate.now(),
        subjectId = "test-subject",
        subjectType = "USER_ID",
      ),
      DigitalServicesQueryRequest(
        auditEventType = STAFF,
        startDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now(),
        who = "who",
      ),
      DigitalServicesQueryRequest(
        auditEventType = STAFF,
        startDate = LocalDate.now().minusDays(1),
        who = "who",
      ),
      DigitalServicesQueryRequest(
        auditEventType = STAFF,
        endDate = LocalDate.now(),
        who = "who",
      ),
    )

    private fun invalidBaseAuditFilterDto() = Stream.of(
      Arguments.of(
        DigitalServicesQueryRequest(auditEventType = STAFF),
        mapOf(
          "startDate" to "startDate must be provided if endDate is null",
          "endDate" to "endDate must be provided if startDate is null",
          "who" to "If 'who' is null, then 'subjectId' and 'subjectType' must be populated",
        ),
      ),

      Arguments.of(
        DigitalServicesQueryRequest(
          auditEventType = STAFF,
          startDate = LocalDate.now(),
          endDate = LocalDate.now().plusDays(1),
          who = "someone",
        ),
        mapOf("endDate" to "endDate must not be in the future"),
      ),

      Arguments.of(
        DigitalServicesQueryRequest(
          auditEventType = STAFF,
          startDate = LocalDate.now().plusDays(1),
          who = "someone",
        ),
        mapOf("startDate" to "startDate must not be in the future"),
      ),

      Arguments.of(
        DigitalServicesQueryRequest(
          auditEventType = STAFF,
          startDate = LocalDate.now(),
          endDate = LocalDate.now().minusDays(1),
          who = "someone",
        ),
        mapOf("startDate" to "startDate must be before endDate"),
      ),

      Arguments.of(
        DigitalServicesQueryRequest(
          auditEventType = STAFF,
          startDate = LocalDate.now().minusDays(1),
          endDate = LocalDate.now(),
          subjectId = "test-subject",
        ),
        mapOf("subjectType" to "Both subjectId and subjectType must be populated together or left null"),
      ),
      Arguments.of(
        DigitalServicesQueryRequest(
          auditEventType = STAFF,
          startDate = LocalDate.now().minusDays(1),
          endDate = LocalDate.now(),
          subjectType = "PERSON",
        ),
        mapOf("subjectId" to "Both subjectId and subjectType must be populated together or left null"),
      ),
      Arguments.of(
        DigitalServicesQueryRequest(
          auditEventType = STAFF,
          startDate = LocalDate.now().minusDays(1),
          endDate = LocalDate.now(),
        ),
        mapOf("who" to "If 'who' is null, then 'subjectId' and 'subjectType' must be populated"),
      ),
    )
  }
}
