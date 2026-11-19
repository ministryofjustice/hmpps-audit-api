package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PersonOnProbationAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.PrisonerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.stream.Stream

@Service
class AuditConsolidationWriter(
  private val parquetWriter: AuditParquetWriter,
  private val entityManager: EntityManager,
  private val staffAuditRepository: StaffAuditRepository,
  private val prisonerAuditRepository: PrisonerAuditRepository,
  private val personOnProbationAuditRepository: PersonOnProbationAuditRepository,
) {
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
  fun write(type: AuditEventType, date: LocalDate, file: Path): Long {
    val start = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    val end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

    return when (type) {
      AuditEventType.STAFF -> writeEvents(
        staffAuditRepository.findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end),
        file,
      ) { it.toAuditEvent() }
      AuditEventType.PRISONER -> writeEvents(
        prisonerAuditRepository.findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end),
        file,
      ) { it.toAuditEvent() }
      AuditEventType.PERSON_ON_PROBATION -> writeEvents(
        personOnProbationAuditRepository.findAllByWhenGreaterThanEqualAndWhenLessThanOrderByWhenAscIdAsc(start, end),
        file,
      ) { it.toAuditEvent() }
    }
  }

  private fun <T> writeEvents(auditEvents: Stream<T>, file: Path, toEvent: (T) -> AuditEvent): Long = auditEvents.use {
    parquetWriter.write(
      auditEvents.iterator().asSequence().map { auditEvent ->
        toEvent(auditEvent).also { entityManager.detach(auditEvent) }
      },
      file,
    )
  }
}
