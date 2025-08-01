package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.AthenaProperties
import uk.gov.justice.digital.hmpps.hmppsauditapi.config.trackEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.jpa.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.HMPPSAuditListener.AuditEvent
import uk.gov.justice.digital.hmpps.hmppsauditapi.listeners.model.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AthenaQueryResponse
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.AuditFilterDto
import uk.gov.justice.digital.hmpps.hmppsauditapi.model.DigitalServicesQueryRequest
import uk.gov.justice.digital.hmpps.hmppsauditapi.resource.AuditDto
import java.util.UUID

@Service
class AuditService(
  private val telemetryClient: TelemetryClient,
  private val auditRepository: AuditRepository,
  private val auditS3Client: AuditS3Client,
  private val auditAthenaClient: AuditAthenaClient,
  @Value("\${hmpps.repository.saveToS3Bucket}") private val saveToS3Bucket: Boolean,

) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun saveAuditEvent(auditEvent: AuditEvent, athenaProperties: AthenaProperties) {
    if (saveToS3Bucket || athenaProperties.auditEventType == AuditEventType.PRISONER) {
      auditEvent.id = UUID.randomUUID()
      auditS3Client.save(auditEvent, athenaProperties.s3BucketName)
      auditAthenaClient.addPartitionForEvent(auditEvent, athenaProperties)
    } else {
      auditRepository.save(auditEvent)
    }
    telemetryClient.trackEvent(athenaProperties.auditEventType.description, auditEvent.asMap())
  }

  fun findAll(): List<AuditDto> = auditRepository.findAll(Sort.by(DESC, "when")).map { AuditDto(it) }

  fun findPage(
    pageable: Pageable = Pageable.unpaged(),
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
      return auditRepository.findPage(
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
        .map { AuditDto(it) }
    }
  }

  fun triggerQuery(
    queryRequest: DigitalServicesQueryRequest,
    auditEventType: AuditEventType,
  ): AthenaQueryResponse = auditAthenaClient.triggerQuery(queryRequest, auditEventType)

  fun getQueryResults(queryExecutionId: String): AthenaQueryResponse = auditAthenaClient.getAuditEventsQueryResults(queryExecutionId)
}

private fun AuditEvent.asMap(): Map<String, String> {
  val items = mutableMapOf("what" to what, "when" to `when`.toString())
  items.addIfNotNull("who", who)
  items.addIfNotNull("operationId", operationId)
  items.addIfNotNull("subjectId", subjectId)
  items.addIfNotNull("subjectType", subjectType)
  items.addIfNotNull("correlationId", correlationId)
  items.addIfNotNull("service", service)
  return items.toMap()
}
