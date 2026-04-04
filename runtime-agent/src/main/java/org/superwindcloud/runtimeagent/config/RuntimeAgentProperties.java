package org.superwindcloud.runtimeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime-agent")
public record RuntimeAgentProperties(
    String apiToken,
    String dockerCommand,
    String managedNetwork,
    String localAppsPath,
    String localDataPath,
    String localDomain) {}
