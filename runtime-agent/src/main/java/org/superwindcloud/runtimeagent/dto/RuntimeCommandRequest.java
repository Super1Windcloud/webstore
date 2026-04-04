package org.superwindcloud.runtimeagent.dto;

import jakarta.validation.constraints.NotBlank;

public record RuntimeCommandRequest(String port, String appDomain, String localDomain) {

  public String normalizedPort() {
    return port == null ? "" : port.trim();
  }

  public boolean hasPort() {
    return !normalizedPort().isBlank() && !"Unknown".equalsIgnoreCase(normalizedPort());
  }

  @NotBlank
  public String localDomain() {
    return localDomain == null ? "" : localDomain.trim();
  }
}
