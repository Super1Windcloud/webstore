package org.superwindcloud.webstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webstore.security")
public record SecurityProperties(String jwtSecret, long jwtExpirationHours) {}
