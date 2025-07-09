package uk.gov.justice.digital.hmpps.hmppsauditapi.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.hmppsauditapi.services.ParquetValidatorService

@RestController
@RequestMapping("/parquet")
class ParquetValidatorController(private val validatorService: ParquetValidatorService) {

    @PostMapping("/start")
    fun startValidation(): ResponseEntity<String> {
        validatorService.clearResults()
        validatorService.validateAsync()
        return ResponseEntity.ok("Parquet validation started")
    }

    @GetMapping("/results")
    fun getResults(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(validatorService.getResults())
    }
}
