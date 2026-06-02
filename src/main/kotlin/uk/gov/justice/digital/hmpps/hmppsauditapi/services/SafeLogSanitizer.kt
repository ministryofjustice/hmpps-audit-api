package uk.gov.justice.digital.hmpps.hmppsauditapi.services

internal object SafeLogSanitizer {
  private val controlCharacters = Regex("[\\u0000-\\u001F\\u007F]")

  fun sanitize(value: Any?): String? {
    val raw = value?.toString() ?: return null
    return controlCharacters.replace(raw) { match ->
      when (match.value) {
        "\n" -> "\\n"
        "\r" -> "\\r"
        "\t" -> "\\t"
        else -> "?"
      }
    }
  }
}
