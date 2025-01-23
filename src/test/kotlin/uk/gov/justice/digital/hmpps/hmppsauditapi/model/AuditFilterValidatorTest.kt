package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsauditapi.IntegrationTest
import uk.gov.justice.digital.hmpps.hmppsauditapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsauditapi.integration.S3TestConfig
import uk.gov.justice.digital.hmpps.hmppsauditapi.integration.endtoend.CommandLineProfilesResolver
import java.time.Instant
import java.util.stream.Stream

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTest.SqsConfig::class, JwtAuthHelper::class, S3TestConfig::class)
@ActiveProfiles(resolver = CommandLineProfilesResolver::class)
@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
class AuditFilterValidatorTest {

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class AuditFilterCases {

    @Autowired
    private lateinit var validator: Validator

    private fun validBaseAuditFilterDto() =
      listOf(
        BaseAuditFilterDto(
          startDateTime = Instant.now().minusSeconds(3600),
          endDateTime = Instant.now(),
          subjectId = "test-subject",
          subjectType = "USER_ID",
        ),
        BaseAuditFilterDto(
          startDateTime = Instant.now().minusSeconds(3600),
          subjectId = "test-subject",
          subjectType = "USER_ID",
        ),
        BaseAuditFilterDto(
          endDateTime = Instant.now(),
          subjectId = "test-subject",
          subjectType = "USER_ID",
        ),
        BaseAuditFilterDto(
          startDateTime = Instant.now().minusSeconds(3600),
          endDateTime = Instant.now(),
          who = "who",
        ),
        BaseAuditFilterDto(
          startDateTime = Instant.now().minusSeconds(3600),
          who = "who",
        ),
        BaseAuditFilterDto(
          endDateTime = Instant.now(),
          who = "who",
        ),
      )

    private fun invalidBaseAuditFilterDto() =
      Stream.of(
        Arguments.of(
          BaseAuditFilterDto(),
          mapOf(
            "startDateTime" to "startDateTime must be provided if endDateTime is null",
            "endDateTime" to "endDateTime must be provided if startDateTime is null",
            "who" to "If who is null then subjectId and subjectType must be populated",
          ),
        ),

        Arguments.of(
          BaseAuditFilterDto(
            startDateTime = Instant.now().minusSeconds(3600),
            endDateTime = Instant.now().plusSeconds(3600),
            who = "someone"
          ),
          mapOf("endDateTime" to "endDateTime must not be in the future"),
        ),

        Arguments.of(
          BaseAuditFilterDto(
            startDateTime = Instant.now().plusSeconds(10),
            who = "someone"
          ),
          mapOf("startDateTime" to "startDateTime must not be in the future"),
        ),

        Arguments.of(
          BaseAuditFilterDto(
            startDateTime = Instant.now().plusSeconds(3600),
            endDateTime = Instant.now(),
            who = "someone"
          ),
          mapOf("startDateTime" to "startDateTime must be before endDateTime"),
        ),

        Arguments.of(
          BaseAuditFilterDto(
            startDateTime = Instant.now().minusSeconds(10),
            endDateTime = Instant.now().minusSeconds(1),
            subjectId = "test-subject",
          ),
          mapOf("subjectType" to "Both subjectId and subjectType must be populated together or left null"),
        ),
        Arguments.of(
          BaseAuditFilterDto(
            startDateTime = Instant.now().minusSeconds(10),
            endDateTime = Instant.now().minusSeconds(1),
            subjectType = "PERSON",
          ),
          mapOf("subjectId" to "Both subjectId and subjectType must be populated together or left null"),
        ),
        Arguments.of(
          BaseAuditFilterDto(
            startDateTime = Instant.now().minusSeconds(10),
            endDateTime = Instant.now().minusSeconds(1),
          ),
          mapOf("who" to "If who is null then subjectId and subjectType must be populated")
        )
      )

    @ParameterizedTest
    @MethodSource("validBaseAuditFilterDto")
    internal fun `should be valid`(baseAuditFilterDto: BaseAuditFilterDto) {
      val violations = validator.validate(baseAuditFilterDto)
      assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidBaseAuditFilterDto")
    internal fun `should be invalid`(baseAuditFilterDto: BaseAuditFilterDto, expectedErrorMessages: Map<String, String>) {
      val errorMessages : Map<String, String> = validator.validate(baseAuditFilterDto).map { it.propertyPath.toString() to it.message }.toMap()
      assertThat(errorMessages).containsExactlyInAnyOrderEntriesOf(expectedErrorMessages)
    }
  }
}
