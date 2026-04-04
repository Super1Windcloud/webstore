package org.superwindcloud.webstore.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.domain.AppRole;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.dto.RegistrationForm;
import org.superwindcloud.webstore.repository.UserAccountRepository;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public UserAccount register(RegistrationForm form) {
    if (userAccountRepository.existsByUsername(form.getUsername().trim())) {
      throw new IllegalArgumentException("Username is already in use");
    }
    if (userAccountRepository.existsByEmail(form.getEmail().trim().toLowerCase())) {
      throw new IllegalArgumentException("Email is already in use");
    }

    UserAccount userAccount =
        new UserAccount(
            form.getUsername().trim(),
            form.getEmail().trim().toLowerCase(),
            passwordEncoder.encode(form.getPassword()),
            AppRole.USER);
    return userAccountRepository.save(userAccount);
  }
}
