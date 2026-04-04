package org.superwindcloud.webstore.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.service.CurrentUserService;

@Controller
public class SettingsController {

  private final CurrentUserService currentUserService;

  public SettingsController(CurrentUserService currentUserService) {
    this.currentUserService = currentUserService;
  }

  @GetMapping("/settings")
  public String settings(Authentication authentication, Model model) {
    UserAccount user = currentUserService.requireUser(authentication);
    model.addAttribute("pageTitle", "设置");
    model.addAttribute("activeNav", "settings");
    model.addAttribute("username", user.getUsername());
    model.addAttribute("email", user.getEmail());
    model.addAttribute("createdAt", user.getCreatedAt());
    return "settings";
  }
}
