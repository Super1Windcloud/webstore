package org.superwindcloud.webstore.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.superwindcloud.webstore.domain.AppDefinition;
import org.superwindcloud.webstore.domain.InstalledApp;
import org.superwindcloud.webstore.domain.InstalledAppStatus;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.model.AppStoreCard;
import org.superwindcloud.webstore.model.DashboardSummary;
import org.superwindcloud.webstore.model.InstalledAppView;
import org.superwindcloud.webstore.repository.AppDefinitionRepository;
import org.superwindcloud.webstore.repository.InstalledAppRepository;

@Service
public class AppCatalogService {

  private final AppDefinitionRepository appDefinitionRepository;
  private final InstalledAppRepository installedAppRepository;

  public AppCatalogService(
      AppDefinitionRepository appDefinitionRepository,
      InstalledAppRepository installedAppRepository) {
    this.appDefinitionRepository = appDefinitionRepository;
    this.installedAppRepository = installedAppRepository;
  }

  @Transactional(readOnly = true)
  public List<AppStoreCard> getStoreCards(UserAccount user) {
    Map<Long, InstalledApp> installedByAppId =
        installedAppRepository.findAllByUserOrderByInstalledAtDesc(user).stream()
            .collect(
                Collectors.toMap(
                    installedApp -> installedApp.getAppDefinition().getId(),
                    Function.identity(),
                    (left, right) -> left));

    return appDefinitionRepository.findAll().stream()
        .map(
            app -> {
              InstalledApp installedApp = installedByAppId.get(app.getId());
              return new AppStoreCard(
                  app.getSlug(),
                  app.getName(),
                  app.getCategory(),
                  app.getDescription(),
                  app.getAccentColor(),
                  app.getIcon(),
                  installedApp != null,
                  installedApp != null ? installedApp.getStatus() : null);
            })
        .toList();
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
                    installedApp.getStatus(),
                    installedApp.getInstalledAt()))
        .toList();
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

  private AppDefinition getAppDefinition(String slug) {
    return appDefinitionRepository
        .findBySlug(slug)
        .orElseThrow(() -> new IllegalArgumentException("App not found"));
  }
}
