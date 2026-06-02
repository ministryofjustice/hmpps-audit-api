package uk.gov.justice.digital.hmpps.hmppsauditapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SafeLogSanitizerTest {

  @Test
  fun `escapes newline carriage return and tab characters`() {
    val sanitized = SafeLogSanitizer.sanitize("line1\nline2\rcell\tvalue")

    assertThat(sanitized).isEqualTo("line1\\nline2\\rcell\\tvalue")
  }

  @Test
  fun `replaces other control characters`() {
    val sanitized = SafeLogSanitizer.sanitize("safe\u0000unsafe")

    assertThat(sanitized).isEqualTo("safe?unsafe")
  }

  @Test
  fun `returns null for null values`() {
    assertThat(SafeLogSanitizer.sanitize(null)).isNull()
  }
}
