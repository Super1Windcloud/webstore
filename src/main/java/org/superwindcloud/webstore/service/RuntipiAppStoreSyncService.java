package org.superwindcloud.webstore.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.superwindcloud.webstore.config.AppStoreProperties;
import org.superwindcloud.webstore.domain.AppDefinition;
import org.superwindcloud.webstore.domain.InstalledAppStatus;
import org.superwindcloud.webstore.model.AppStoreCard;
import org.superwindcloud.webstore.model.AppStoreDetail;
import org.superwindcloud.webstore.repository.AppDefinitionRepository;

@Service
public class RuntipiAppStoreSyncService {

  private static final Logger log = LoggerFactory.getLogger(RuntipiAppStoreSyncService.class);
  private static final Pattern CONFIG_PATH = Pattern.compile("^[^/]+/apps/([^/]+)/config\\.json$");
  private static final String FALLBACK_CATEGORY = "General";
  private static final String FALLBACK_DESCRIPTION = "Imported from runtipi-appstore.";

  private final AppStoreProperties appStoreProperties;
  private final AppDefinitionRepository appDefinitionRepository;
  private final HttpClient httpClient;
  private final JsonParser jsonParser;
  private volatile Instant lastSyncAt;

  public RuntipiAppStoreSyncService(
      AppStoreProperties appStoreProperties, AppDefinitionRepository appDefinitionRepository) {
    this.appStoreProperties = appStoreProperties;
    this.appDefinitionRepository = appDefinitionRepository;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.jsonParser = JsonParserFactory.getJsonParser();
  }

  public void syncIfStale() {
    if (!appStoreProperties.syncEnabled()) {
      return;
    }

    Instant syncThreshold =
        Instant.now().minus(Duration.ofHours(appStoreProperties.refreshHours()));
    if (lastSyncAt != null
        && lastSyncAt.isAfter(syncThreshold)
        && appDefinitionRepository.count() > 0) {
      return;
    }

    synchronized (this) {
      if (lastSyncAt != null
          && lastSyncAt.isAfter(syncThreshold)
          && appDefinitionRepository.count() > 0) {
        return;
      }

      try {
        syncCatalog();
      } catch (Exception ex) {
        log.warn("Failed to sync runtipi app store catalog: {}", ex.getMessage());
      }
    }
  }

  public List<AppStoreCard> fetchLiveStoreCards(
      Map<String, InstalledAppStatus> installedStatusBySlug) {
    try {
      return fetchRemoteCards(installedStatusBySlug);
    } catch (Exception ex) {
      log.warn("Falling back to cached app store catalog: {}", ex.getMessage());
      syncIfStale();
      return appDefinitionRepository.findAll().stream()
          .map(
              app ->
                  new AppStoreCard(
                      app.getSlug(),
                      app.getName(),
                      app.getCategory(),
                      app.getDescription(),
                      app.getAccentColor(),
                      app.getIcon(),
                      logoUrlFor(app.getSlug()),
                      installedStatusBySlug.containsKey(app.getSlug()),
                      installedStatusBySlug.get(app.getSlug())))
          .toList();
    }
  }

  public AppStoreDetail fetchLiveStoreDetail(
      String slug, Map<String, InstalledAppStatus> installedStatusBySlug) {
    try {
      return fetchRemoteDetail(slug, installedStatusBySlug);
    } catch (Exception ex) {
      log.warn("Falling back to cached app detail for {}: {}", slug, ex.getMessage());
      AppDefinition appDefinition =
          appDefinitionRepository
              .findBySlug(slug)
              .orElseThrow(() -> new IllegalArgumentException("App not found"));
      return new AppStoreDetail(
          appDefinition.getSlug(),
          appDefinition.getName(),
          appDefinition.getCategory(),
          appDefinition.getDescription(),
          appDefinition.getDescription(),
          "",
          "",
          appDefinition.getAccentColor(),
          appDefinition.getIcon(),
          logoUrlFor(appDefinition.getSlug()),
          installedStatusBySlug.containsKey(appDefinition.getSlug()),
          installedStatusBySlug.get(appDefinition.getSlug()),
          "Unknown",
          "Unknown",
          null,
          "Unknown",
          "Unknown",
          List.of());
    }
  }

  @Transactional
  public void syncCatalog() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(appStoreProperties.zipUrl()))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "webstore-dev")
            .GET()
            .build();

    HttpResponse<InputStream> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() >= 400) {
      throw new IOException("Unexpected HTTP status: " + response.statusCode());
    }

    List<AppDefinitionPayload> apps = parseApps(response.body());
    for (AppDefinitionPayload payload : apps) {
      upsert(payload);
    }

    lastSyncAt = Instant.now();
    log.info("Synced {} app definitions from runtipi-appstore", apps.size());
  }

  private List<AppStoreCard> fetchRemoteCards(Map<String, InstalledAppStatus> installedStatusBySlug)
      throws IOException, InterruptedException {
    List<AppDefinitionPayload> apps = parseApps(downloadZipStream());
    return apps.stream()
        .map(
            app ->
                new AppStoreCard(
                    app.slug(),
                    app.name(),
                    app.category(),
                    app.description(),
                    app.accentColor(),
                    app.icon(),
                    logoUrlFor(app.slug()),
                    installedStatusBySlug.containsKey(app.slug()),
                    installedStatusBySlug.get(app.slug())))
        .toList();
  }

  private AppStoreDetail fetchRemoteDetail(
      String slug, Map<String, InstalledAppStatus> installedStatusBySlug)
      throws IOException, InterruptedException {
    return parseApps(downloadZipStream()).stream()
        .filter(app -> app.slug().equals(slug))
        .findFirst()
        .map(
            app ->
                new AppStoreDetail(
                    app.slug(),
                    app.name(),
                    app.category(),
                    app.description(),
                    app.longDescription(),
                    app.metadataDescription(),
                    "",
                    app.accentColor(),
                    app.icon(),
                    logoUrlFor(app.slug()),
                    installedStatusBySlug.containsKey(app.slug()),
                    installedStatusBySlug.get(app.slug()),
                    app.version(),
                    app.author(),
                    app.sourceUrl(),
                    app.port(),
                    app.tipiVersion(),
                    app.architectures()))
        .orElseThrow(() -> new IllegalArgumentException("App not found"));
  }

  private InputStream downloadZipStream() throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(appStoreProperties.zipUrl()))
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "webstore-dev")
            .GET()
            .build();

    HttpResponse<InputStream> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

    if (response.statusCode() >= 400) {
      throw new IOException("Unexpected HTTP status: " + response.statusCode());
    }
    return response.body();
  }

  private List<AppDefinitionPayload> parseApps(InputStream responseBody) throws IOException {
    Map<String, String> descriptionsBySlug = new HashMap<>();
    List<ConfigEntry> configEntries = new ArrayList<>();

    try (ZipInputStream zipInputStream = new ZipInputStream(responseBody)) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        String entryName = entry.getName();
        Matcher matcher = CONFIG_PATH.matcher(entryName);
        if (matcher.matches()) {
          configEntries.add(
              new ConfigEntry(
                  matcher.group(1),
                  jsonParser.parseMap(
                      new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8))));
          continue;
        }
        Matcher descriptionMatcher =
            Pattern.compile("^[^/]+/apps/([^/]+)/metadata/description\\.md$").matcher(entryName);
        if (descriptionMatcher.matches()) {
          descriptionsBySlug.put(
              descriptionMatcher.group(1),
              new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8).trim());
        }
      }
    }

    List<AppDefinitionPayload> apps = new ArrayList<>();
    for (ConfigEntry entry : configEntries) {
      Map<String, Object> config = entry.config();
      if (!Boolean.TRUE.equals(config.get("available"))) {
        continue;
      }

      String slug = stringValue(config.get("id")).orElse(entry.slug());
      String name = stringValue(config.get("name")).orElse(slug);
      String category =
          Optional.ofNullable(config.get("categories"))
              .filter(List.class::isInstance)
              .map(List.class::cast)
              .filter(categories -> !categories.isEmpty())
              .map(categories -> stringValue(categories.get(0)).orElse(FALLBACK_CATEGORY))
              .map(this::normalizeCategory)
              .orElse(FALLBACK_CATEGORY);
      String description =
          truncate(
              firstNonBlank(
                  stringValue(config.get("short_desc")).orElse(null),
                  stringValue(config.get("description")).orElse(null),
                  FALLBACK_DESCRIPTION),
              255);
      String longDescription =
          firstNonBlank(
              stringValue(config.get("description")).orElse(null),
              stringValue(config.get("short_desc")).orElse(null),
              FALLBACK_DESCRIPTION);
      String metadataDescription = descriptionsBySlug.getOrDefault(slug, "");
      String version = stringValue(config.get("version")).orElse("Unknown");
      String author = stringValue(config.get("author")).orElse("Unknown");
      String sourceUrl = stringValue(config.get("source")).orElse(null);
      String port = config.containsKey("port") ? String.valueOf(config.get("port")) : "Unknown";
      String tipiVersion =
          firstNonBlank(
              stringValue(config.get("min_tipi_version")).orElse(null),
              config.containsKey("tipi_version")
                  ? String.valueOf(config.get("tipi_version"))
                  : null,
              "Unknown");
      List<String> architectures = extractArchitectures(config.get("supported_architectures"));

      apps.add(
          new AppDefinitionPayload(
              slug,
              truncate(name, 120),
              category,
              description,
              longDescription,
              metadataDescription,
              colorFor(slug),
              initialsFor(name),
              version,
              author,
              sourceUrl,
              port,
              tipiVersion,
              architectures));
    }

    return apps;
  }

  private Optional<String> stringValue(Object value) {
    if (value instanceof String string && !string.isBlank()) {
      return Optional.of(string);
    }
    return Optional.empty();
  }

  private List<String> extractArchitectures(Object value) {
    if (!(value instanceof List<?> values)) {
      return List.of();
    }

    List<String> architectures = new ArrayList<>();
    for (Object entry : values) {
      stringValue(entry).ifPresent(architectures::add);
    }
    return architectures;
  }

  private void upsert(AppDefinitionPayload payload) {
    appDefinitionRepository
        .findBySlug(payload.slug())
        .ifPresentOrElse(
            existing ->
                existing.updateCatalogMetadata(
                    payload.name(),
                    payload.category(),
                    payload.description(),
                    payload.accentColor(),
                    payload.icon()),
            () ->
                appDefinitionRepository.save(
                    new AppDefinition(
                        payload.slug(),
                        payload.name(),
                        payload.category(),
                        payload.description(),
                        payload.accentColor(),
                        payload.icon())));
  }

  private String normalizeCategory(String category) {
    if (category == null || category.isBlank()) {
      return FALLBACK_CATEGORY;
    }
    String normalized = category.replace('-', ' ').trim();
    return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength - 1) + "…";
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String colorFor(String slug) {
    String hex = HexFormat.of().formatHex(slug.getBytes(StandardCharsets.UTF_8));
    if (hex.length() < 6) {
      hex = (hex + "5aa9ff").substring(0, 6);
    }
    return "#" + hex.substring(0, 6);
  }

  private String logoUrlFor(String slug) {
    return appStoreProperties.rawBaseUrl() + "/" + slug + "/metadata/logo.jpg";
  }

  private String initialsFor(String name) {
    String sanitized = name.replaceAll("[^A-Za-z0-9 ]", " ").trim();
    if (sanitized.isBlank()) {
      return "AP";
    }

    String[] parts = sanitized.split("\\s+");
    StringBuilder initials = new StringBuilder();
    for (String part : parts) {
      if (!part.isBlank()) {
        initials.append(Character.toUpperCase(part.charAt(0)));
      }
      if (initials.length() == 2) {
        break;
      }
    }

    if (initials.isEmpty()) {
      return sanitized.substring(0, Math.min(2, sanitized.length())).toUpperCase(Locale.ROOT);
    }
    return initials.toString();
  }

  private record AppDefinitionPayload(
      String slug,
      String name,
      String category,
      String description,
      String longDescription,
      String metadataDescription,
      String accentColor,
      String icon,
      String version,
      String author,
      String sourceUrl,
      String port,
      String tipiVersion,
      List<String> architectures) {}

  private record ConfigEntry(String slug, Map<String, Object> config) {}
}
