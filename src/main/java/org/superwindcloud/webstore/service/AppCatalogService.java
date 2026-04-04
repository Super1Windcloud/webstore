package org.superwindcloud.webstore.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.superwindcloud.webstore.config.AppStoreProperties;
import org.superwindcloud.webstore.domain.AppDefinition;
import org.superwindcloud.webstore.domain.InstalledApp;
import org.superwindcloud.webstore.domain.InstalledAppStatus;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.model.AppStoreCard;
import org.superwindcloud.webstore.model.AppStoreDetail;
import org.superwindcloud.webstore.model.DashboardSummary;
import org.superwindcloud.webstore.model.InstalledAppView;
import org.superwindcloud.webstore.repository.AppDefinitionRepository;
import org.superwindcloud.webstore.repository.InstalledAppRepository;

@Service
public class AppCatalogService {

  private final AppDefinitionRepository appDefinitionRepository;
  private final InstalledAppRepository installedAppRepository;
  private final AppStoreProperties appStoreProperties;
  private final RuntipiAppStoreSyncService runtipiAppStoreSyncService;
  private final MarkdownRendererService markdownRendererService;

  public AppCatalogService(
      AppDefinitionRepository appDefinitionRepository,
      InstalledAppRepository installedAppRepository,
      AppStoreProperties appStoreProperties,
      RuntipiAppStoreSyncService runtipiAppStoreSyncService,
      MarkdownRendererService markdownRendererService) {
    this.appDefinitionRepository = appDefinitionRepository;
    this.installedAppRepository = installedAppRepository;
    this.appStoreProperties = appStoreProperties;
    this.runtipiAppStoreSyncService = runtipiAppStoreSyncService;
    this.markdownRendererService = markdownRendererService;
  }

  @Transactional(readOnly = true)
  public List<AppStoreCard> getStoreCards(UserAccount user) {
    return mapStoreCards(appDefinitionRepository.findAll(), installedAppStatusBySlug(user));
  }

  @Transactional(readOnly = true)
  public List<AppStoreCard> getLiveStoreCards(UserAccount user) {
    return runtipiAppStoreSyncService.fetchLiveStoreCards(installedAppStatusBySlug(user));
  }

  @Transactional(readOnly = true)
  public AppStoreDetail getLiveStoreDetail(UserAccount user, String slug) {
    AppStoreDetail detail =
        runtipiAppStoreSyncService.fetchLiveStoreDetail(slug, installedAppStatusBySlug(user));
    return new AppStoreDetail(
        detail.slug(),
        detail.name(),
        detail.category(),
        detail.description(),
        detail.longDescription(),
        detail.metadataDescription(),
        markdownRendererService.render(detail.metadataDescription()),
        detail.accentColor(),
        detail.icon(),
        detail.logoUrl(),
        appHomeUrl(detail.slug(), detail.port()),
        detail.installed(),
        detail.status(),
        detail.version(),
        detail.author(),
        detail.sourceUrl(),
        detail.port(),
        detail.tipiVersion(),
        detail.architectures());
  }

  @Transactional(readOnly = true)
  public List<InstalledAppView> getInstalledApps(UserAccount user) {
    return installedAppRepository.findAllByUserOrderByInstalledAtDesc(user).stream()
        .map(
            installedApp ->
                new InstalledAppView(
                    installedApp.getAppDefinition().getSlug(),
                    installedApp.getAppDefinition().getName(),
                    installedApp.getAppDefinition().getCategory(),
                    installedApp.getAppDefinition().getDescription(),
                    installedApp.getAppDefinition().getAccentColor(),
                    installedApp.getAppDefinition().getIcon(),
                    logoUrlFor(installedApp.getAppDefinition().getSlug()),
                    appHomeUrl(installedApp.getAppDefinition().getSlug(), null),
                    installedApp.getStatus(),
                    installedApp.getInstalledAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public AppStoreDetail getInstalledAppDetail(UserAccount user, String slug) {
    AppStoreDetail detail = getLiveStoreDetail(user, slug);
    if (!detail.installed()) {
      throw new IllegalArgumentException("App is not installed");
    }
    return detail;
  }

  @Transactional(readOnly = true)
  public DashboardSummary getDashboardSummary(UserAccount user) {
    long totalApps = appDefinitionRepository.count();
    List<InstalledApp> installedApps =
        installedAppRepository.findAllByUserOrderByInstalledAtDesc(user);
    long runningApps =
        installedApps.stream().filter(app -> app.getStatus() == InstalledAppStatus.RUNNING).count();
    long stoppedApps = installedApps.size() - runningApps;
    return new DashboardSummary(totalApps, installedApps.size(), runningApps, stoppedApps);
  }

  @Transactional
  public void installApp(UserAccount user, String slug) {
    AppDefinition appDefinition = getAppDefinition(slug);
    installedAppRepository
        .findByUserAndAppDefinition(user, appDefinition)
        .orElseGet(
            () ->
                installedAppRepository.save(
                    new InstalledApp(user, appDefinition, InstalledAppStatus.RUNNING)));
  }

  @Transactional
  public void uninstallApp(UserAccount user, String slug) {
    AppDefinition appDefinition = getAppDefinition(slug);
    installedAppRepository
        .findByUserAndAppDefinition(user, appDefinition)
        .ifPresent(installedAppRepository::delete);
  }

  @Transactional
  public void updateStatus(UserAccount user, String slug, InstalledAppStatus status) {
    AppDefinition appDefinition = getAppDefinition(slug);
    InstalledApp installedApp =
        installedAppRepository
            .findByUserAndAppDefinition(user, appDefinition)
            .orElseThrow(() -> new IllegalArgumentException("App is not installed"));
    installedApp.setStatus(status);
  }

  @Transactional
  public void updateAllStatuses(UserAccount user, InstalledAppStatus status) {
    installedAppRepository
        .findAllByUserOrderByInstalledAtDesc(user)
        .forEach(app -> app.setStatus(status));
  }

  private List<AppStoreCard> mapStoreCards(
      List<AppDefinition> appDefinitions, Map<String, InstalledAppStatus> installedStatusBySlug) {
    return appDefinitions.stream()
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

  private Map<String, InstalledAppStatus> installedAppStatusBySlug(UserAccount user) {
    Map<Long, InstalledApp> installedByAppId =
        installedAppRepository.findAllByUserOrderByInstalledAtDesc(user).stream()
            .collect(
                Collectors.toMap(
                    installedApp -> installedApp.getAppDefinition().getId(),
                    Function.identity(),
                    (left, right) -> left));
    return installedByAppId.values().stream()
        .collect(
            Collectors.toMap(
                installedApp -> installedApp.getAppDefinition().getSlug(),
                InstalledApp::getStatus,
                (left, right) -> left));
  }

  private String logoUrlFor(String slug) {
    return appStoreProperties.rawBaseUrl() + "/" + slug + "/metadata/logo.jpg";
  }

  private String appHomeUrl(String slug, String port) {
    if (port != null && !"Unknown".equalsIgnoreCase(port)) {
      return "http://localhost:" + port;
    }
    return "http://localhost/" + slug;
  }

  private AppDefinition getAppDefinition(String slug) {
    return appDefinitionRepository
        .findBySlug(slug)
        .orElseThrow(() -> new IllegalArgumentException("App not found"));
  }
}
