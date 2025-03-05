package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.hmppsauditapi.exception.FieldValidationException
import java.time.LocalDate

class DigitalServicesQueryRequestValidator : ConstraintValidator<ValidDigitalServicesQueryRequest, DigitalServicesQueryRequest> {

  override fun isValid(dto: DigitalServicesQueryRequest?, context: ConstraintValidatorContext): Boolean {
    if (dto == null) return false
    context.disableDefaultConstraintViolation()
    val now = LocalDate.now()
    val errors = mutableMapOf<String, String>()

    if (dto.startDate == null && dto.endDate == null) {
      errors["startDateTime"] = "startDateTime must be provided if endDateTime is null"
      errors["endDateTime"] = "endDateTime must be provided if startDateTime is null"
    }

    if (dto.who == null && dto.subjectId == null && dto.subjectType == null) {
      errors["who"] = "If 'who' is null, then 'subjectId' and 'subjectType' must be populated"
    }

    if (dto.endDate?.isAfter(now) == true) {
      errors["endDateTime"] = "endDateTime must not be in the future"
    }

    if (dto.startDate?.isAfter(now) == true) {
      errors["startDateTime"] = "startDateTime must not be in the future"
    }

    if (dto.startDate != null && dto.endDate != null && dto.startDate.isAfter(dto.endDate)) {
      errors["startDateTime"] = "startDateTime must be before endDateTime"
    }

    if ((dto.subjectId != null && dto.subjectType == null) || (dto.subjectId == null && dto.subjectType != null)) {
      val propertyNode = if (dto.subjectId == null) "subjectId" else "subjectType"
      errors[propertyNode] = "Both subjectId and subjectType must be populated together or left null"
    }

    if (errors.isNotEmpty()) {
      throw FieldValidationException(errors)
    }

    return true
  }
}
