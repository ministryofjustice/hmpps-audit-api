package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AuditConsolidationConfig
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class AuditConsolidationServiceTest {
  @Mock
  private lateinit var consolidationWriter: AuditConsolidationWriter

  @Mock
  private lateinit var auditS3Client: AuditS3Client

  @Mock
  private lateinit var executor: TaskExecutor

  @Mock
  private lateinit var propertiesFactory: AthenaPropertiesFactory

  private val queryDate = LocalDate.of(2025, 1, 1)
  private val eventType = AuditEventType.STAFF

  private lateinit var service: AuditConsolidationService

  @BeforeEach
  fun setUp() {
    AuditEventType.entries.forEach { eventType ->
      whenever(propertiesFactory.getProperties(eventType)).thenReturn(
        AthenaProperties(eventType, "${eventType.name.lowercase()}-audit", "", "", "", ""),
      )
    }
    service = AuditConsolidationService(
      AuditConsolidationConfig(true, "consolidations", setOf(eventType), propertiesFactory),
      consolidationWriter,
      auditS3Client,
      executor,
    )
  }

  @Test
  fun `disabled consolidations reject submission without starting work`() {
    val disabled = AuditConsolidationService(
      AuditConsolidationConfig(false, "", setOf(eventType), propertiesFactory),
      consolidationWriter,
      auditS3Client,
      executor,
    )

    val exception = assertThrows<ResponseStatusException> { disabled.startConsolidationJob(eventType, queryDate) }

    assertThat(exception.statusCode.value()).isEqualTo(503)
    verifyNoInteractions(consolidationWriter, auditS3Client, executor)
  }

  @Test
  fun `submission queues work without generating or uploading on the request thread`() {
    service.startConsolidationJob(eventType, queryDate)

    verify(executor).execute(any())
    verifyNoInteractions(consolidationWriter, auditS3Client)
  }

  @Test
  fun `full executor returns service unavailable`() {
    doThrow(TaskRejectedException("Full")).whenever(executor).execute(any())

    val exception = assertThrows<ResponseStatusException> { service.startConsolidationJob(eventType, queryDate) }

    assertThat(exception.statusCode.value()).isEqualTo(503)
    verifyNoInteractions(consolidationWriter, auditS3Client)
  }

  @Test
  fun `consolidation uploads the generated file then removes it`() {
    runTasksImmediately()

    service.startConsolidationJob(eventType, queryDate)

    val file = argumentCaptor<Path>()
    verify(consolidationWriter).write(eq(eventType), eq(queryDate), file.capture())
    verify(auditS3Client).saveConsolidatedAuditEvents(file.firstValue, "consolidations", key(eventType))
    assertThat(Files.exists(file.firstValue)).isFalse()
  }

  @Test
  fun `generation failure removes the temporary file without uploading`() {
    runTasksImmediately()
    whenever(consolidationWriter.write(any(), any(), any())).thenThrow(IllegalStateException("Generation failed"))

    service.startConsolidationJob(eventType, queryDate)

    val file = argumentCaptor<Path>()
    verify(consolidationWriter).write(eq(eventType), eq(queryDate), file.capture())
    assertThat(Files.exists(file.firstValue)).isFalse()
    verifyNoInteractions(auditS3Client)
  }

  @Test
  fun `background upload failure is handled and the temporary file is removed`() {
    doThrow(IllegalStateException("Upload failed")).whenever(auditS3Client).saveConsolidatedAuditEvents(any<Path>(), any(), any())

    service.startConsolidationJob(eventType, queryDate)

    val task = argumentCaptor<Runnable>()
    verify(executor).execute(task.capture())
    task.firstValue.run()
    val file = argumentCaptor<Path>()
    verify(auditS3Client).saveConsolidatedAuditEvents(file.capture(), eq("consolidations"), eq(key(eventType)))
    assertThat(Files.exists(file.firstValue)).isFalse()
  }

  @Test
  fun `other event types are rejected until enabled for rollout`() {
    listOf(AuditEventType.PRISONER, AuditEventType.PERSON_ON_PROBATION).forEach { disabledType ->
      assertThat(assertThrows<ResponseStatusException> { service.startConsolidationJob(disabledType, queryDate) }.statusCode.value()).isEqualTo(400)
    }
    verifyNoInteractions(executor, consolidationWriter, auditS3Client)
  }

  @Test
  fun `enabled event types flow through the same consolidation pipeline`() {
    runTasksImmediately()
    val allTypes = AuditConsolidationService(
      AuditConsolidationConfig(true, "consolidations", AuditEventType.entries.toSet(), propertiesFactory),
      consolidationWriter,
      auditS3Client,
      executor,
    )
    AuditEventType.entries.forEach { enabledType ->
      allTypes.startConsolidationJob(enabledType, queryDate)
      verify(consolidationWriter).write(eq(enabledType), eq(queryDate), any())
      verify(auditS3Client).saveConsolidatedAuditEvents(any<Path>(), eq("consolidations"), eq(key(enabledType)))
    }
  }

  private fun key(type: AuditEventType) = "${type.name.lowercase()}/year=2025/month=1/day=1/audit-events.parquet"

  private fun runTasksImmediately() {
    doAnswer {
      it.getArgument<Runnable>(0).run()
      null
    }.whenever(executor).execute(any())
  }
}
