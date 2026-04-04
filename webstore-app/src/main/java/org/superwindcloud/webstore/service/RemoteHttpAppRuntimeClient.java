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
  public String install(AppDefinition appDefinition) {
    return execute(appDefinition, "install", "POST");
  }

  @Override
  public String start(AppDefinition appDefinition) {
    return execute(appDefinition, "start", "POST");
  }

  @Override
  public String stop(AppDefinition appDefinition) {
    return execute(appDefinition, "stop", "POST");
  }

  @Override
  public String uninstall(AppDefinition appDefinition) {
    return execute(appDefinition, "", "DELETE");
  }

  private String execute(AppDefinition appDefinition, String action, String method) {
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
      RuntimeCommandResponse runtimeResponse = parseResponse(response.body());
      if (response.statusCode() >= 400) {
        log.warn(
            "Remote runtime request failed for app '{}' with status {} and body:\n{}",
            appDefinition.getSlug(),
            response.statusCode(),
            response.body());
        throw new AppOperationException(
            buildErrorMessage(response.statusCode(), runtimeResponse, response.body()));
      }
      if (!response.body().isBlank()) {
        log.info("Remote runtime output: {}", response.body());
      }
      if (runtimeResponse != null && hasText(runtimeResponse.output())) {
        log.info(
            "Remote runtime parsed output for app '{}':\n{}",
            appDefinition.getSlug(),
            runtimeResponse.output());
      }
      return runtimeResponse == null ? null : runtimeResponse.output();
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

  private RuntimeCommandResponse parseResponse(String responseBody) {
    if (!hasText(responseBody)) {
      return null;
    }
    String status = extractJsonString(responseBody, "status");
    String message = extractJsonString(responseBody, "message");
    String output = extractJsonString(responseBody, "output");
    if (status == null && message == null && output == null) {
      log.warn("Failed to parse remote runtime response body as JSON: {}", responseBody);
      return null;
    }
    return new RuntimeCommandResponse(status, message, output);
  }

  private String buildErrorMessage(
      int statusCode, RuntimeCommandResponse runtimeResponse, String responseBody) {
    if (runtimeResponse != null) {
      StringBuilder builder =
          new StringBuilder("远程运行时调用失败，status=")
              .append(statusCode)
              .append("，消息：")
              .append(runtimeResponse.message());
      if (hasText(runtimeResponse.output())) {
        builder.append("\n\n").append(runtimeResponse.output());
      }
      return builder.toString();
    }
    return "远程运行时调用失败，status=" + statusCode + "，输出：" + responseBody;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String extractJsonString(String json, String fieldName) {
    String marker = "\"" + fieldName + "\":";
    int start = json.indexOf(marker);
    if (start < 0) {
      return null;
    }
    int valueStart = start + marker.length();
    while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
      valueStart++;
    }
    if (valueStart + 4 <= json.length()
        && "null".equals(json.substring(valueStart, valueStart + 4))) {
      return null;
    }
    if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
      return null;
    }

    StringBuilder builder = new StringBuilder();
    boolean escaping = false;
    for (int i = valueStart + 1; i < json.length(); i++) {
      char current = json.charAt(i);
      if (escaping) {
        builder.append(unescapeJsonChar(current));
        escaping = false;
        continue;
      }
      if (current == '\\') {
        escaping = true;
        continue;
      }
      if (current == '"') {
        return builder.toString();
      }
      builder.append(current);
    }
    return null;
  }

  private char unescapeJsonChar(char current) {
    return switch (current) {
      case 'n' -> '\n';
      case 'r' -> '\r';
      case 't' -> '\t';
      case '"' -> '"';
      case '\\' -> '\\';
      default -> current;
    };
  }

  private record RuntimeCommandRequest(String port, String appDomain, String localDomain) {}

  private record RuntimeCommandResponse(String status, String message, String output) {}
}
