package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaPropertiesFactory
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.StaffAuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.model.toAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.asMap
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.toStaffAuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.model.AuditDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.AuditQueueService.Companion.log
import java.util.UUID

@Service
class StaffAuditService(
  private val telemetryClient: TelemetryClient,
  private val staffAuditRepository: StaffAuditRepository,
  private val auditS3Client: AuditS3Client,
  private val auditAthenaClient: AuditAthenaClient,
  private val athenaPropertiesFactory: AthenaPropertiesFactory,
  @param:Value("\${hmpps.repository.saveToS3Bucket}") private val saveToS3Bucket: Boolean,

) : AuditServiceInterface {
  override fun saveAuditEvent(
    auditEvent: AuditEvent,
  ) {
    if (saveToS3Bucket) {
      val athenaProperties: AthenaProperties = athenaPropertiesFactory.getProperties(AuditEventType.STAFF)
      auditEvent.id = UUID.randomUUID()
      auditS3Client.save(auditEvent, athenaProperties.s3BucketName)
      auditAthenaClient.addPartitionForEvent(auditEvent, athenaProperties)
    }

    staffAuditRepository.save(auditEvent.toStaffAuditEvent())

    telemetryClient.trackEvent(AuditEventType.STAFF.description, auditEvent.asMap())
  }

  override fun findAll(): List<AuditDto> = staffAuditRepository.findAll(Sort.by(DESC, "when")).map { AuditDto(it.toAuditEvent()) }

  override fun findPage(
    pageable: Pageable,
    auditFilterDto: AuditFilterDto,
  ): Page<AuditDto> {
    with(auditFilterDto) {
      log.info(
        "Searching audit events by startDate {} endDate {} service {} subjectId {} subjectType {} correlationId {} what {} who {}",
        startDateTime,
        endDateTime,
        service,
        subjectId,
        subjectType,
        correlationId,
        what,
        who,
      )
      return staffAuditRepository.findPage(
        pageable,
        startDateTime,
        endDateTime,
        service,
        subjectId,
        subjectType,
        correlationId,
        what,
        who,
      )
        .map { AuditDto(it.toAuditEvent()) }
    }
  }

  override fun triggerQuery(
    queryRequest: AuditQueryRequest,
    auditEventType: AuditEventType,
  ): AuditQueryResponse = auditAthenaClient.triggerQuery(queryRequest, auditEventType)

  override fun getQueryResults(queryExecutionId: String): AuditQueryResponse = auditAthenaClient.getAuditEventsQueryResults(queryExecutionId)
}
