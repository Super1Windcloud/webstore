package org.superwindcloud.webstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.service.AppCatalogService;
import org.superwindcloud.webstore.service.CurrentUserService;
import org.superwindcloud.webstore.service.RuntipiAppStoreSyncService;
import org.superwindcloud.webstore.service.SystemMetricsService;

@Controller
public class DashboardController {

  private final CurrentUserService currentUserService;
  private final AppCatalogService appCatalogService;
  private final SystemMetricsService systemMetricsService;
  private final RuntipiAppStoreSyncService runtipiAppStoreSyncService;

  public DashboardController(
      CurrentUserService currentUserService,
      AppCatalogService appCatalogService,
      SystemMetricsService systemMetricsService,
      RuntipiAppStoreSyncService runtipiAppStoreSyncService) {
    this.currentUserService = currentUserService;
    this.appCatalogService = appCatalogService;
    this.systemMetricsService = systemMetricsService;
    this.runtipiAppStoreSyncService = runtipiAppStoreSyncService;
  }

  @GetMapping("/dashboard")
  public String dashboard(Authentication authentication, Model model) {
    runtipiAppStoreSyncService.syncIfStale();
    UserAccount user = currentUserService.requireUser(authentication);
    model.addAttribute("pageTitle", "控制面板");
    model.addAttribute("activeNav", "dashboard");
    model.addAttribute("username", user.getUsername());
    model.addAttribute("summary", appCatalogService.getDashboardSummary(user));
    model.addAttribute("systemOverview", systemMetricsService.getSystemOverview());
    model.addAttribute(
        "recentApps", appCatalogService.getInstalledApps(user).stream().limit(3).toList());
    model.addAttribute(
        "storeCards", appCatalogService.getStoreCards(user).stream().limit(4).toList());
    return "dashboard";
  }
}
