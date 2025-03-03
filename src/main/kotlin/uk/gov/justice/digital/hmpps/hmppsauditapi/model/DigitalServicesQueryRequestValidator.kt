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

    if (dto.startDate == null && dto.endDate == null) {
      isValid = false
      context.buildConstraintViolationWithTemplate("startDateTime must be provided if endDateTime is null")
        .addPropertyNode("startDateTime")
        .addConstraintViolation()

      context.buildConstraintViolationWithTemplate("endDateTime must be provided if startDateTime is null")
        .addPropertyNode("endDateTime")
        .addConstraintViolation()
    }

    if (dto.who == null && dto.subjectId == null && dto.subjectType == null) {
      isValid = false
      context.buildConstraintViolationWithTemplate("If who is null then subjectId and subjectType must be populated")
        .addPropertyNode("who")
        .addConstraintViolation()
    }

    if (dto.endDate?.isAfter(now) == true) {
      isValid = false
      context.buildConstraintViolationWithTemplate("endDateTime must not be in the future")
        .addPropertyNode("endDateTime")
        .addConstraintViolation()
    }

    if (dto.startDate?.isAfter(now) == true) {
      isValid = false
      context.buildConstraintViolationWithTemplate("startDateTime must not be in the future")
        .addPropertyNode("startDateTime")
        .addConstraintViolation()
    }

    if (dto.startDate != null && dto.endDate != null && dto.startDate.isAfter(dto.endDate)) {
      isValid = false
      context.buildConstraintViolationWithTemplate("startDateTime must be before endDateTime")
        .addPropertyNode("startDateTime")
        .addConstraintViolation()
    }

    if ((dto.subjectId != null && dto.subjectType == null) || (dto.subjectId == null && dto.subjectType != null)) {
      val propertyNode = if (dto.subjectId == null) "subjectId" else "subjectType"
      isValid = false
      context.buildConstraintViolationWithTemplate("Both subjectId and subjectType must be populated together or left null")
        .addPropertyNode(propertyNode)
        .addConstraintViolation()
    }

    return isValid
  }
}
