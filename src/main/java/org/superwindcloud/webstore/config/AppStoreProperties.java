package org.superwindcloud.webstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webstore.app-store")
public record AppStoreProperties(
    boolean syncEnabled,
    String zipUrl,
    String rawBaseUrl,
    long refreshHours,
    String localAppsPath,
    String localDataPath,
    String dockerCommand,
    String localDomain,
    String managedNetwork) {}
