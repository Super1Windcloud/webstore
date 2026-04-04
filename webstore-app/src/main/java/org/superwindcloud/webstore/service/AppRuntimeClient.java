package org.superwindcloud.webstore.service;

import org.superwindcloud.webstore.domain.AppDefinition;

public interface AppRuntimeClient {

  void install(AppDefinition appDefinition);

  void start(AppDefinition appDefinition);

  void stop(AppDefinition appDefinition);

  void uninstall(AppDefinition appDefinition);
}
