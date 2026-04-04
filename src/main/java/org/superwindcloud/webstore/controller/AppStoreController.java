package org.superwindcloud.webstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.service.AppCatalogService;
import org.superwindcloud.webstore.service.CurrentUserService;
import org.superwindcloud.webstore.service.RuntipiAppStoreSyncService;

@Controller
public class AppStoreController {

  private final CurrentUserService currentUserService;
  private final AppCatalogService appCatalogService;
  private final RuntipiAppStoreSyncService runtipiAppStoreSyncService;

  public AppStoreController(
      CurrentUserService currentUserService,
      AppCatalogService appCatalogService,
      RuntipiAppStoreSyncService runtipiAppStoreSyncService) {
    this.currentUserService = currentUserService;
    this.appCatalogService = appCatalogService;
    this.runtipiAppStoreSyncService = runtipiAppStoreSyncService;
  }

  @GetMapping("/app-store")
  public String appStore(Authentication authentication, Model model) {
    runtipiAppStoreSyncService.syncIfStale();
    UserAccount user = currentUserService.requireUser(authentication);
    model.addAttribute("pageTitle", "AppStore");
    model.addAttribute("activeNav", "app-store");
    model.addAttribute("username", user.getUsername());
    model.addAttribute("apps", appCatalogService.getLiveStoreCards(user));
    return "app-store";
  }

  @PostMapping("/app-store/{slug}/install")
  public String install(@PathVariable String slug, Authentication authentication) {
    appCatalogService.installApp(currentUserService.requireUser(authentication), slug);
    return "redirect:/app-store";
  }
}
