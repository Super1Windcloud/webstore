package org.superwindcloud.webstore.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.superwindcloud.webstore.config.AppStoreProperties;
import org.superwindcloud.webstore.config.RuntimeProperties;
import org.superwindcloud.webstore.domain.AppDefinition;

@Component
public class RemoteHttpAppRuntimeClient implements AppRuntimeClient {

  private static final Logger log = LoggerFactory.getLogger(RemoteHttpAppRuntimeClient.class);

  private final RuntimeProperties runtimeProperties;
  private final AppStoreProperties appStoreProperties;
  private final HttpClient httpClient;

  public RemoteHttpAppRuntimeClient(
      RuntimeProperties runtimeProperties, AppStoreProperties appStoreProperties) {
    this.runtimeProperties = runtimeProperties;
    this.appStoreProperties = appStoreProperties;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(
                Duration.ofSeconds(Math.max(1, runtimeProperties.connectTimeoutSeconds())))
            .build();
  }

  @Override
  public void install(AppDefinition appDefinition) {
    execute(appDefinition, "install", "POST");
  }

  @Override
  public void start(AppDefinition appDefinition) {
    execute(appDefinition, "start", "POST");
  }

  @Override
  public void stop(AppDefinition appDefinition) {
    execute(appDefinition, "stop", "POST");
  }

  @Override
  public void uninstall(AppDefinition appDefinition) {
    execute(appDefinition, "", "DELETE");
  }

  private void execute(AppDefinition appDefinition, String action, String method) {
    String slug = encodedSlug(appDefinition.getSlug());
    String path = action.isBlank() ? "/api/apps/" + slug : "/api/apps/" + slug + "/" + action;
    URI uri = URI.create(normalizedBaseUrl() + path);
    String requestBody =
        serializeRequest(
            new RuntimeCommandRequest(
                appDefinition.getPort(),
                appDomain(appDefinition),
                appStoreProperties.localDomain()));

    HttpRequest.Builder builder =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(Math.max(1, runtimeProperties.readTimeoutSeconds())))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");
    if (runtimeProperties.apiToken() != null && !runtimeProperties.apiToken().isBlank()) {
      builder.header("Authorization", "Bearer " + runtimeProperties.apiToken());
    }

    if ("DELETE".equals(method)) {
      builder.method("DELETE", HttpRequest.BodyPublishers.ofString(requestBody));
    } else {
      builder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
    }

    log.info(
        "Executing remote runtime request for app '{}' via {} {}",
        appDefinition.getSlug(),
        method,
        uri);

    try {
      HttpResponse<String> response =
          httpClient.send(
              builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
        throw new AppOperationException(
            "远程运行时调用失败，status=" + response.statusCode() + "，输出：" + response.body());
      }
      if (!response.body().isBlank()) {
        log.info("Remote runtime output: {}", response.body());
      }
    } catch (IOException ex) {
      throw new AppOperationException("无法连接远程运行时服务: " + uri, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppOperationException("远程运行时请求被中断", ex);
    }
  }

  private String normalizedBaseUrl() {
    if (runtimeProperties.baseUrl() == null || runtimeProperties.baseUrl().isBlank()) {
      throw new AppOperationException("远程运行时未配置 base-url");
    }
    return runtimeProperties.baseUrl().endsWith("/")
        ? runtimeProperties.baseUrl().substring(0, runtimeProperties.baseUrl().length() - 1)
        : runtimeProperties.baseUrl();
  }

  private String encodedSlug(String slug) {
    return URLEncoder.encode(slug, StandardCharsets.UTF_8);
  }

  private String serializeRequest(RuntimeCommandRequest request) {
    return "{"
        + "\"port\":"
        + jsonString(request.port())
        + ",\"appDomain\":"
        + jsonString(request.appDomain())
        + ",\"localDomain\":"
        + jsonString(request.localDomain())
        + "}";
  }

  private String appDomain(AppDefinition appDefinition) {
    return appDefinition.getSlug() + "." + appStoreProperties.localDomain();
  }

  private String jsonString(String value) {
    if (value == null) {
      return "null";
    }
    return "\""
        + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
        + "\"";
  }

  private record RuntimeCommandRequest(String port, String appDomain, String localDomain) {}
}
