package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("aws-testing")
@RequestMapping("/internal/dev-testing")
class DevTestingController
