package org.superwindcloud.runtimeagent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.superwindcloud.runtimeagent.config.RuntimeAgentProperties;

@Component
public class RuntimeAgentAuthFilter extends OncePerRequestFilter {

  private final RuntimeAgentProperties runtimeAgentProperties;

  public RuntimeAgentAuthFilter(RuntimeAgentProperties runtimeAgentProperties) {
    this.runtimeAgentProperties = runtimeAgentProperties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return "/api/health".equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String expectedToken = runtimeAgentProperties.apiToken();
    if (expectedToken == null || expectedToken.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    String expectedHeader = "Bearer " + expectedToken;
    if (!expectedHeader.equals(authorization)) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }
}
