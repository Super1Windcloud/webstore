package org.superwindcloud.webstore.service;

import org.superwindcloud.webstore.domain.AppDefinition;

public interface AppRuntimeClient {

  String install(AppDefinition appDefinition);

  String start(AppDefinition appDefinition);

  String stop(AppDefinition appDefinition);

  String uninstall(AppDefinition appDefinition);
}
