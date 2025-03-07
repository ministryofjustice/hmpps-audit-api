package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.time.LocalDate

class DigitalServicesQueryRequestValidator : ConstraintValidator<ValidDigitalServicesQueryRequest, DigitalServicesQueryRequest> {

  override fun isValid(dto: DigitalServicesQueryRequest?, context: ConstraintValidatorContext): Boolean {
    if (dto == null) return false
    context.disableDefaultConstraintViolation()
    val now = LocalDate.now()
    var isValid = true

    fun addViolation(field: String, message: String) {
      context.buildConstraintViolationWithTemplate(message)
        .addPropertyNode(field)
        .addConstraintViolation()
      isValid = false
    }

    if (dto.startDate == null && dto.endDate == null) {
      addViolation("startDate", "startDate must be provided if endDate is null")
      addViolation("endDate", "endDate must be provided if startDate is null")
    }

    if (dto.who == null && dto.subjectId == null && dto.subjectType == null) {
      addViolation("who", "If 'who' is null, then 'subjectId' and 'subjectType' must be populated")
    }

    if (dto.endDate?.isAfter(now) == true) {
      addViolation("endDate", "endDate must not be in the future")
    }

    if (dto.startDate?.isAfter(now) == true) {
      addViolation("startDate", "startDate must not be in the future")
    }

    if (dto.startDate != null && dto.endDate != null && dto.startDate.isAfter(dto.endDate)) {
      addViolation("startDate", "startDate must be before endDate")
    }

    if ((dto.subjectId != null && dto.subjectType == null) || (dto.subjectId == null && dto.subjectType != null)) {
      val field = if (dto.subjectId == null) "subjectId" else "subjectType"
      addViolation(field, "Both subjectId and subjectType must be populated together or left null")
    }

    return isValid
  }
}
