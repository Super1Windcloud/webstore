package org.superwindcloud.webstore.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.config.RuntimeProperties;
import org.superwindcloud.webstore.domain.AppDefinition;

@Service
public class AppRuntimeService {

  private final RuntimeProperties runtimeProperties;
  private final LocalDockerAppRuntimeClient localDockerAppRuntimeClient;
  private final RemoteHttpAppRuntimeClient remoteHttpAppRuntimeClient;

  public AppRuntimeService(
      RuntimeProperties runtimeProperties,
      LocalDockerAppRuntimeClient localDockerAppRuntimeClient,
      RemoteHttpAppRuntimeClient remoteHttpAppRuntimeClient) {
    this.runtimeProperties = runtimeProperties;
    this.localDockerAppRuntimeClient = localDockerAppRuntimeClient;
    this.remoteHttpAppRuntimeClient = remoteHttpAppRuntimeClient;
  }

  public String install(AppDefinition appDefinition) {
    return currentClient().install(appDefinition);
  }

  public String start(AppDefinition appDefinition) {
    return currentClient().start(appDefinition);
  }

  public String stop(AppDefinition appDefinition) {
    return currentClient().stop(appDefinition);
  }

  public String uninstall(AppDefinition appDefinition) {
    return currentClient().uninstall(appDefinition);
  }

  private AppRuntimeClient currentClient() {
    String mode = normalizeMode(runtimeProperties.mode());
    if ("remote".equals(mode)) {
      return remoteHttpAppRuntimeClient;
    }
    if ("local".equals(mode)) {
      return localDockerAppRuntimeClient;
    }
    throw new AppOperationException("不支持的运行时模式: " + runtimeProperties.mode());
  }

  private String normalizeMode(String mode) {
    return mode == null ? "local" : mode.trim().toLowerCase(Locale.ROOT);
  }
}
