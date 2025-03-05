package uk.gov.justice.digital.hmpps.hmppsauditapi.exception

class FieldValidationException(val errors: Map<String, String>) : RuntimeException("Validation failed")
