package org.superwindcloud.webstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.superwindcloud.webstore.domain.InstalledAppStatus;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.service.AppCatalogService;
import org.superwindcloud.webstore.service.AppOperationException;
import org.superwindcloud.webstore.service.CurrentUserService;
import org.superwindcloud.webstore.service.RuntipiAppStoreSyncService;

@Controller
public class MyAppsController {

  private static final Logger log = LoggerFactory.getLogger(MyAppsController.class);

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
    runtipiAppStoreSyncService.syncIfStale();
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
  public String startApp(
      @PathVariable String slug,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    try {
      String output =
          appCatalogService.updateStatus(
              currentUserService.requireUser(authentication), slug, InstalledAppStatus.RUNNING);
      logOperationResult("start", slug, output);
      redirectAttributes.addFlashAttribute("toastType", "success");
      redirectAttributes.addFlashAttribute("toastMessage", successMessage("应用已启动", output));
    } catch (AppOperationException | IllegalArgumentException ex) {
      log.warn("WebStore start failed for app '{}': {}", slug, ex.getMessage(), ex);
      redirectAttributes.addFlashAttribute("toastType", "danger");
      redirectAttributes.addFlashAttribute("toastMessage", ex.getMessage());
      return "redirect:/my-apps";
    }
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/{slug}/stop")
  public String stopApp(
      @PathVariable String slug,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    try {
      String output =
          appCatalogService.updateStatus(
              currentUserService.requireUser(authentication), slug, InstalledAppStatus.STOPPED);
      logOperationResult("stop", slug, output);
      redirectAttributes.addFlashAttribute("toastType", "warning");
      redirectAttributes.addFlashAttribute("toastMessage", successMessage("应用已停止", output));
    } catch (AppOperationException | IllegalArgumentException ex) {
      log.warn("WebStore stop failed for app '{}': {}", slug, ex.getMessage(), ex);
      redirectAttributes.addFlashAttribute("toastType", "danger");
      redirectAttributes.addFlashAttribute("toastMessage", ex.getMessage());
      return "redirect:/my-apps";
    }
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/{slug}/uninstall")
  public String uninstallApp(
      @PathVariable String slug,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    try {
      String output =
          appCatalogService.uninstallApp(currentUserService.requireUser(authentication), slug);
      logOperationResult("uninstall", slug, output);
      redirectAttributes.addFlashAttribute("toastType", "danger");
      redirectAttributes.addFlashAttribute("toastMessage", successMessage("应用已卸载", output));
    } catch (AppOperationException | IllegalArgumentException ex) {
      log.warn("WebStore uninstall failed for app '{}': {}", slug, ex.getMessage(), ex);
      redirectAttributes.addFlashAttribute("toastType", "danger");
      redirectAttributes.addFlashAttribute("toastMessage", ex.getMessage());
      return "redirect:/my-apps";
    }
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/start-all")
  public String startAllApps(Authentication authentication, RedirectAttributes redirectAttributes) {
    try {
      String output =
          appCatalogService.updateAllStatuses(
              currentUserService.requireUser(authentication), InstalledAppStatus.RUNNING);
      logBulkOperationResult("start-all", output);
      redirectAttributes.addFlashAttribute("toastType", "success");
      redirectAttributes.addFlashAttribute("toastMessage", successMessage("所有应用已启动", output));
    } catch (AppOperationException | IllegalArgumentException ex) {
      log.warn("WebStore start-all failed: {}", ex.getMessage(), ex);
      redirectAttributes.addFlashAttribute("toastType", "danger");
      redirectAttributes.addFlashAttribute("toastMessage", ex.getMessage());
      return "redirect:/my-apps";
    }
    return "redirect:/my-apps";
  }

  @PostMapping("/my-apps/stop-all")
  public String stopAllApps(Authentication authentication, RedirectAttributes redirectAttributes) {
    try {
      String output =
          appCatalogService.updateAllStatuses(
              currentUserService.requireUser(authentication), InstalledAppStatus.STOPPED);
      logBulkOperationResult("stop-all", output);
      redirectAttributes.addFlashAttribute("toastType", "warning");
      redirectAttributes.addFlashAttribute("toastMessage", successMessage("所有应用已停止", output));
    } catch (AppOperationException | IllegalArgumentException ex) {
      log.warn("WebStore stop-all failed: {}", ex.getMessage(), ex);
      redirectAttributes.addFlashAttribute("toastType", "danger");
      redirectAttributes.addFlashAttribute("toastMessage", ex.getMessage());
      return "redirect:/my-apps";
    }
    return "redirect:/my-apps";
  }

  private String successMessage(String message, String output) {
    if (output == null || output.isBlank()) {
      return message;
    }
    return message + "\n\n" + output;
  }

  private void logOperationResult(String action, String slug, String output) {
    if (output == null || output.isBlank()) {
      log.info("WebStore {} completed for app '{}' with no runtime output", action, slug);
      return;
    }
    log.info("WebStore {} output for app '{}':\n{}", action, slug, output);
  }

  private void logBulkOperationResult(String action, String output) {
    if (output == null || output.isBlank()) {
      log.info("WebStore {} completed with no runtime output", action);
      return;
    }
    log.info("WebStore {} output:\n{}", action, output);
  }
}
