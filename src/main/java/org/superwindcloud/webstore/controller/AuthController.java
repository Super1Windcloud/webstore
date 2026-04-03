package org.superwindcloud.webstore.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.superwindcloud.webstore.dto.LoginForm;
import org.superwindcloud.webstore.dto.RegistrationForm;
import org.superwindcloud.webstore.security.JwtAuthenticationFilter;
import org.superwindcloud.webstore.service.AuthService;
import org.superwindcloud.webstore.service.JwtService;

@Controller
public class AuthController {

  private final AuthService authService;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthController(
      AuthService authService, AuthenticationManager authenticationManager, JwtService jwtService) {
    this.authService = authService;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
  }

  @GetMapping("/")
  public String home(Authentication authentication) {
    return isLoggedIn(authentication) ? "redirect:/dashboard" : "redirect:/login";
  }

  @GetMapping("/login")
  public String loginPage(Authentication authentication, Model model) {
    if (isLoggedIn(authentication)) {
      return "redirect:/dashboard";
    }
    if (!model.containsAttribute("loginForm")) {
      model.addAttribute("loginForm", new LoginForm());
    }
    model.addAttribute("pageTitle", "登录");
    return "auth/login";
  }

  @PostMapping("/login")
  public String login(
      @Valid @ModelAttribute("loginForm") LoginForm loginForm,
      BindingResult bindingResult,
      HttpServletResponse response,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("pageTitle", "登录");
      return "auth/login";
    }

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginForm.getUsername().trim(), loginForm.getPassword()));
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      addAuthCookie(response, jwtService.generateToken(userDetails));
      return "redirect:/dashboard";
    } catch (BadCredentialsException ex) {
      model.addAttribute("pageTitle", "登录");
      model.addAttribute("authError", "用户名或密码错误");
      return "auth/login";
    }
  }

  @GetMapping("/register")
  public String registerPage(Authentication authentication, Model model) {
    if (isLoggedIn(authentication)) {
      return "redirect:/dashboard";
    }
    if (!model.containsAttribute("registrationForm")) {
      model.addAttribute("registrationForm", new RegistrationForm());
    }
    model.addAttribute("pageTitle", "注册");
    return "auth/register";
  }

  @PostMapping("/register")
  public String register(
      @Valid @ModelAttribute("registrationForm") RegistrationForm registrationForm,
      BindingResult bindingResult,
      HttpServletResponse response,
      Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("pageTitle", "注册");
      return "auth/register";
    }

    try {
      authService.register(registrationForm);
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  registrationForm.getUsername().trim(), registrationForm.getPassword()));
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      addAuthCookie(response, jwtService.generateToken(userDetails));
      return "redirect:/dashboard";
    } catch (IllegalArgumentException ex) {
      model.addAttribute("pageTitle", "注册");
      model.addAttribute("registerError", ex.getMessage());
      return "auth/register";
    }
  }

  @PostMapping("/logout")
  public String logout(HttpServletResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .maxAge(0)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return "redirect:/login";
  }

  private void addAuthCookie(HttpServletResponse response, String token) {
    ResponseCookie cookie =
        ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .maxAge(60L * 60L * 24L)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private boolean isLoggedIn(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
