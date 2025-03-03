package uk.gov.justice.digital.hmpps.hmppsauditapi.model

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DigitalServicesQueryRequestValidator::class])
annotation class ValidDigitalServicesQueryRequest(
  val message: String = "Invalid audit filter",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)
