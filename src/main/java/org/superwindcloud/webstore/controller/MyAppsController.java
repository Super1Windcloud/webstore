package org.superwindcloud.webstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.superwindcloud.webstore.domain.InstalledAppStatus;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.service.AppCatalogService;
import org.superwindcloud.webstore.service.CurrentUserService;
import org.superwindcloud.webstore.service.RuntipiAppStoreSyncService;

@Controller
public class MyAppsController {

  private final CurrentUserService currentUserService;
  private final AppCatalogService appCatalogService;
  private final RuntipiAppStoreSyncService runtipiAppStoreSyncService;

  public MyAppsController(
      CurrentUserService currentUserService,
      AppCatalogService appCatalogService,
      RuntipiAppStoreSyncService runtipiAppStoreSyncService) {
    this.currentUserService = currentUserService;
    this.appCatalogService = appCatalogService;
    this.runtipiAppStoreSyncService = runtipiAppStoreSyncService;
  }

  @GetMapping("/my-apps")
  public String myApps(Authentication authentication, Model model) {
    UserAccount user = currentUserService.requireUser(authentication);
    model.addAttribute("pageTitle", "我的App");
    model.addAttribute("activeNav", "my-apps");
    model.addAttribute("username", user.getUsername());
    model.addAttribute("apps", appCatalogService.getInstalledApps(user));
    return "my-apps";
  }

  @GetMapping("/my-apps/{slug}")
  public String appDetail(@PathVariable String slug, Authentication authentication, Model model) {
    runtipiAppStoreSyncService.syncIfStale();
    UserAccount user = currentUserService.requireUser(authentication);
    model.addAttribute("pageTitle", "App 详情");
    model.addAttribute("activeNav", "my-apps");
    model.addAttribute("username", user.getUsername());
    model.addAttribute("app", appCatalogService.getInstalledAppDetail(user, slug));
    return "app-detail";
  }

  @PostMapping("/my-apps/{slug}/start")
  public String startApp(@PathVariable String slug, Authentication authentication) {
    appCatalogService.updateStatus(
        currentUserService.requireUser(authentication), slug, InstalledAppStatus.RUNNING);
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/{slug}/stop")
  public String stopApp(@PathVariable String slug, Authentication authentication) {
    appCatalogService.updateStatus(
        currentUserService.requireUser(authentication), slug, InstalledAppStatus.STOPPED);
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/{slug}/uninstall")
  public String uninstallApp(@PathVariable String slug, Authentication authentication) {
    appCatalogService.uninstallApp(currentUserService.requireUser(authentication), slug);
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/start-all")
  public String startAllApps(Authentication authentication) {
    appCatalogService.updateAllStatuses(
        currentUserService.requireUser(authentication), InstalledAppStatus.RUNNING);
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/stop-all")
  public String stopAllApps(Authentication authentication) {
    appCatalogService.updateAllStatuses(
        currentUserService.requireUser(authentication), InstalledAppStatus.STOPPED);
    return "redirect:/my-apps";
  }
}
