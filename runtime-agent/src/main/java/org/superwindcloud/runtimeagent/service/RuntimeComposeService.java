package org.superwindcloud.runtimeagent.service;

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
import org.superwindcloud.runtimeagent.config.RuntimeAgentProperties;
import org.superwindcloud.runtimeagent.dto.RuntimeCommandRequest;
import org.superwindcloud.runtimeagent.exception.RuntimeOperationException;

@Service
public class RuntimeComposeService {

  private static final Logger log = LoggerFactory.getLogger(RuntimeComposeService.class);

  private final RuntimeAgentProperties runtimeAgentProperties;

  public RuntimeComposeService(RuntimeAgentProperties runtimeAgentProperties) {
    this.runtimeAgentProperties = runtimeAgentProperties;
  }

  public ProcessResult install(String slug, RuntimeCommandRequest request) {
    ensureManagedNetwork();
    return runCompose(slug, request, List.of("up", "-d"));
  }

  public ProcessResult start(String slug, RuntimeCommandRequest request) {
    ensureManagedNetwork();
    return runCompose(slug, request, List.of("up", "-d"));
  }

  public ProcessResult stop(String slug, RuntimeCommandRequest request) {
    return runCompose(slug, request, List.of("stop"));
  }

  public ProcessResult uninstall(String slug, RuntimeCommandRequest request) {
    return runCompose(slug, request, List.of("down", "--remove-orphans"));
  }

  private void ensureManagedNetwork() {
    ProcessResult inspect =
        runCommand(
            List.of(
                runtimeAgentProperties.dockerCommand(),
                "network",
                "inspect",
                runtimeAgentProperties.managedNetwork()),
            Map.of(),
            null);
    if (inspect.exitCode() == 0) {
      return;
    }

    log.info("Creating docker network '{}'", runtimeAgentProperties.managedNetwork());
    ProcessResult create =
        runCommand(
            List.of(
                runtimeAgentProperties.dockerCommand(),
                "network",
                "create",
                runtimeAgentProperties.managedNetwork()),
            Map.of(),
            null);
    if (create.exitCode() != 0) {
      throw operationFailed("创建 Docker 网络失败", create);
    }
  }

  private ProcessResult runCompose(String slug, RuntimeCommandRequest request, List<String> args) {
    Path commonComposeFile = Path.of(runtimeAgentProperties.localAppsPath()).getParent().resolve("docker-compose.common.yml");
    Path composeFile = Path.of(runtimeAgentProperties.localAppsPath(), slug, "docker-compose.yml");
    if (!Files.exists(composeFile)) {
      throw new RuntimeOperationException("未找到应用的 docker-compose.yml: " + composeFile);
    }

    Path appDataDir = Path.of(runtimeAgentProperties.localDataPath(), slug);
    try {
      Files.createDirectories(appDataDir);
    } catch (IOException ex) {
      throw new RuntimeOperationException("无法创建应用数据目录: " + appDataDir, ex);
    }

    Map<String, String> env = new LinkedHashMap<>();
    env.put("APP_PORT", normalizedPort(slug, request));
    env.put("APP_DOMAIN", normalizedAppDomain(slug, request));
    env.put("LOCAL_DOMAIN", normalizedLocalDomain(request));
    env.put("APP_DATA_DIR", normalizedPath(appDataDir));
    env.put(
        "ROOT_FOLDER_HOST",
        normalizedPath(Path.of(runtimeAgentProperties.localDataPath()).toAbsolutePath()));

    List<String> command = new ArrayList<>();
    command.add(runtimeAgentProperties.dockerCommand());
    command.add("compose");
    if (Files.exists(commonComposeFile)) {
      command.add("-f");
      command.add(commonComposeFile.toAbsolutePath().toString());
    }
    command.add("-f");
    command.add(composeFile.toAbsolutePath().toString());
    command.addAll(args);

    log.info("Executing runtime-agent command for app '{}': {}", slug, String.join(" ", command));
    ProcessResult result = runCommand(command, env, composeFile.getParent());
    if (result.exitCode() != 0) {
      throw operationFailed("Docker Compose 执行失败", result);
    }
    return result;
  }

  private String normalizedPort(String slug, RuntimeCommandRequest request) {
    if (!request.hasPort()) {
      throw new RuntimeOperationException("应用未配置可暴露端口，无法启动: " + slug);
    }
    return request.normalizedPort();
  }

  private String normalizedAppDomain(String slug, RuntimeCommandRequest request) {
    if (request.appDomain() != null && !request.appDomain().trim().isBlank()) {
      return request.appDomain().trim();
    }
    return slug + "." + normalizedLocalDomain(request);
  }

  private String normalizedLocalDomain(RuntimeCommandRequest request) {
    if (request.localDomain() != null && !request.localDomain().trim().isBlank()) {
      return request.localDomain().trim();
    }
    if (runtimeAgentProperties.localDomain() != null
        && !runtimeAgentProperties.localDomain().isBlank()) {
      return runtimeAgentProperties.localDomain().trim();
    }
    return "localhost";
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
        log.info("Runtime-agent output: {}", output);
      }
      return new ProcessResult(exitCode, output);
    } catch (IOException ex) {
      throw new RuntimeOperationException("无法执行 Docker 命令，请确认 runtime-agent 已正确挂载 Docker", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeOperationException("Docker 命令执行被中断", ex);
    }
  }

  private RuntimeOperationException operationFailed(String message, ProcessResult result) {
    String detail = result.output().isBlank() ? "无输出" : result.output();
    return new RuntimeOperationException(
        message + "，exitCode=" + result.exitCode() + "，输出：" + detail, result.output());
  }

  public record ProcessResult(int exitCode, String output) {}
}
