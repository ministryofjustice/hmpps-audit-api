package uk.gov.justice.digital.hmpps.hmppsauditapi.resource

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class DebugRequestLoggingFilter : OncePerRequestFilter() {
  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    println("DEBUG: Entering filter for ${request.method} ${request.requestURI}")
    filterChain.doFilter(request, response)
  }
}
