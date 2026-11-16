package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import jakarta.persistence.EntityManager
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.io.LocalInputFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PersonOnProbationAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toPersonOnProbationAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toPrisonerAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toStaffAuditEvent
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
class AuditConsolidationWriterTest {
  @Mock
  private lateinit var entityManager: EntityManager

  @Mock
  private lateinit var staff: StaffAuditRepository

  @Mock
  private lateinit var prisoners: PrisonerAuditRepository

  @Mock
  private lateinit var probationAuditRepository: PersonOnProbationAuditRepository

  private lateinit var writer: AuditConsolidationWriter
  private lateinit var consolidationFile: Path

  private val queryDate = LocalDate.of(2025, 6, 1)
  private val start = queryDate.atStartOfDay().toInstant(ZoneOffset.UTC)
  private val end = queryDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
  private val auditEvent = AuditEvent(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    what = "CONSOLIDATION_EVENT",
    `when` = start,
    operationId = "operation",
    subjectId = "subject",
    subjectType = "PERSON",
    correlationId = "correlation",
    who = "USER",
    service = "SERVICE",
    details = """{"key":"value"}""",
  )

  @BeforeEach
  fun setUp() {
    val schema = Schema.Parser().parse(javaClass.getResourceAsStream("/audit_event.avsc"))
    writer = AuditConsolidationWriter(
      AuditParquetWriter(schema),
      entityManager,
      staff,
      prisoners,
      probationAuditRepository,
    )
  }

  @AfterEach
  fun cleanUp() {
    Files.deleteIfExists(consolidationFile)
  }

  @ParameterizedTest
  @EnumSource(AuditEventType::class)
  fun `writes requested repository events to Parquet with every field`(auditEventType: AuditEventType) {
    consolidationFile = Files.createTempFile("audit-consolidation-type-test-", ".parquet")
    when (auditEventType) {
      AuditEventType.STAFF -> given(staff.findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end))
        .willReturn(Stream.of(auditEvent.toStaffAuditEvent()))
      AuditEventType.PRISONER -> given(prisoners.findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end))
        .willReturn(Stream.of(auditEvent.toPrisonerAuditEvent()))
      AuditEventType.PERSON_ON_PROBATION -> given(
        probationAuditRepository.findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end),
      ).willReturn(Stream.of(auditEvent.toPersonOnProbationAuditEvent()))
    }

    assertThat(writer.write(auditEventType, queryDate, consolidationFile)).isEqualTo(1)

    when (auditEventType) {
      AuditEventType.STAFF -> {
        verify(staff).findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end)
        verifyNoInteractions(prisoners, probationAuditRepository)
      }
      AuditEventType.PRISONER -> {
        verify(prisoners).findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end)
        verifyNoInteractions(staff, probationAuditRepository)
      }
      AuditEventType.PERSON_ON_PROBATION -> {
        verify(probationAuditRepository).findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end)
        verifyNoInteractions(staff, prisoners)
      }
    }
    verify(entityManager).detach(
      when (auditEventType) {
        AuditEventType.STAFF -> auditEvent.toStaffAuditEvent()
        AuditEventType.PRISONER -> auditEvent.toPrisonerAuditEvent()
        AuditEventType.PERSON_ON_PROBATION -> auditEvent.toPersonOnProbationAuditEvent()
      },
    )

    AvroParquetReader.builder<GenericRecord>(LocalInputFile(consolidationFile)).build().use { reader ->
      val record = reader.read()
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
      assertThat(reader.read()).isNull()
    }
  }
}
