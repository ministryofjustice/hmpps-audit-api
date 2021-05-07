package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import com.amazonaws.services.sqs.AmazonSQSAsync
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsauditapi.helper.JwtAuthHelper

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var awsSqsClient: AmazonSQSAsync

  @Value("\${sqs.queue.name}")
  protected lateinit var queueName: String

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun setAuthorisation(
    user: String = "hmpps-audit-client",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)
}
