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

  public void install(AppDefinition appDefinition) {
    currentClient().install(appDefinition);
  }

  public void start(AppDefinition appDefinition) {
    currentClient().start(appDefinition);
  }

  public void stop(AppDefinition appDefinition) {
    currentClient().stop(appDefinition);
  }

  public void uninstall(AppDefinition appDefinition) {
    currentClient().uninstall(appDefinition);
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
