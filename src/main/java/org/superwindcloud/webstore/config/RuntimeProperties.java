package org.superwindcloud.webstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webstore.runtime")
public record RuntimeProperties(
    String mode,
    String baseUrl,
    String apiToken,
    long connectTimeoutSeconds,
    long readTimeoutSeconds) {}
