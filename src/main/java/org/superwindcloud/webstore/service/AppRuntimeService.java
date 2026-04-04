package org.superwindcloud.webstore.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.config.AppStoreProperties;
import org.superwindcloud.webstore.domain.AppDefinition;

@Service
public class AppRuntimeService {

  private static final Logger log = LoggerFactory.getLogger(AppRuntimeService.class);

  private final AppStoreProperties appStoreProperties;

  public AppRuntimeService(AppStoreProperties appStoreProperties) {
    this.appStoreProperties = appStoreProperties;
  }

  public void install(AppDefinition appDefinition) {
    ensureManagedNetwork();
    runCompose(appDefinition, List.of("up", "-d"));
  }

  public void start(AppDefinition appDefinition) {
    ensureManagedNetwork();
    runCompose(appDefinition, List.of("up", "-d"));
  }

  public void stop(AppDefinition appDefinition) {
    runCompose(appDefinition, List.of("stop"));
  }

  public void uninstall(AppDefinition appDefinition) {
    runCompose(appDefinition, List.of("down", "--remove-orphans"));
  }

  private void ensureManagedNetwork() {
    String docker = appStoreProperties.dockerCommand();
    String network = appStoreProperties.managedNetwork();

    ProcessResult inspect =
        runCommand(List.of(docker, "network", "inspect", network), Map.of(), null);
    if (inspect.exitCode() == 0) {
      return;
    }

    log.info("Creating docker network '{}'", network);
    ProcessResult create =
        runCommand(List.of(docker, "network", "create", network), Map.of(), null);
    if (create.exitCode() != 0) {
      throw operationFailed("创建 Docker 网络失败", create);
    }
  }

  private void runCompose(AppDefinition appDefinition, List<String> args) {
    Path composeFile =
        Path.of(appStoreProperties.localAppsPath(), appDefinition.getSlug(), "docker-compose.yml");
    if (!Files.exists(composeFile)) {
      throw new AppOperationException("未找到应用的 docker-compose.yml: " + composeFile);
    }

    Path appDataDir = Path.of(appStoreProperties.localDataPath(), appDefinition.getSlug());
    try {
      Files.createDirectories(appDataDir);
    } catch (IOException ex) {
      throw new AppOperationException("无法创建应用数据目录: " + appDataDir, ex);
    }

    Map<String, String> env = new LinkedHashMap<>();
    env.put("APP_PORT", normalizedPort(appDefinition));
    env.put("APP_DOMAIN", appDefinition.getSlug() + "." + appStoreProperties.localDomain());
    env.put("LOCAL_DOMAIN", appStoreProperties.localDomain());
    env.put("APP_DATA_DIR", normalizedPath(appDataDir));
    env.put(
        "ROOT_FOLDER_HOST",
        normalizedPath(Path.of(appStoreProperties.localDataPath()).toAbsolutePath()));

    List<String> command = new ArrayList<>();
    command.add(appStoreProperties.dockerCommand());
    command.add("compose");
    command.add("-f");
    command.add(composeFile.toAbsolutePath().toString());
    command.addAll(args);

    log.info(
        "Executing runtime command for app '{}': {}",
        appDefinition.getSlug(),
        String.join(" ", command));
    ProcessResult result = runCommand(command, env, composeFile.getParent());
    if (result.exitCode() != 0) {
      throw operationFailed("Docker Compose 执行失败", result);
    }
  }

  private String normalizedPort(AppDefinition appDefinition) {
    if (appDefinition.getPort() == null
        || appDefinition.getPort().isBlank()
        || "Unknown".equalsIgnoreCase(appDefinition.getPort())) {
      throw new AppOperationException("应用未配置可暴露端口，无法启动: " + appDefinition.getSlug());
    }
    return appDefinition.getPort();
  }

  private String normalizedPath(Path path) {
    return path.toAbsolutePath().toString().replace('\\', '/');
  }

  private ProcessResult runCommand(List<String> command, Map<String, String> env, Path workDir) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      if (workDir != null) {
        processBuilder.directory(workDir.toFile());
      }
      processBuilder.redirectErrorStream(true);
      processBuilder.environment().putAll(env);

      Process process = processBuilder.start();
      String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      int exitCode = process.waitFor();

      if (!output.isBlank()) {
        log.info("Runtime output: {}", output);
      }
      return new ProcessResult(exitCode, output);
    } catch (IOException ex) {
      throw new AppOperationException("无法执行 Docker 命令，请确认已安装并已加入 PATH", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppOperationException("Docker 命令执行被中断", ex);
    }
  }

  private AppOperationException operationFailed(String message, ProcessResult result) {
    String detail = result.output().isBlank() ? "无输出" : result.output();
    return new AppOperationException(message + "，exitCode=" + result.exitCode() + "，输出：" + detail);
  }

  private record ProcessResult(int exitCode, String output) {}
}
