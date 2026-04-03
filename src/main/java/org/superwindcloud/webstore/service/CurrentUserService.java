package org.superwindcloud.webstore.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.domain.UserAccount;
import org.superwindcloud.webstore.repository.UserAccountRepository;

@Service
public class CurrentUserService {

  private final UserAccountRepository userAccountRepository;

  public CurrentUserService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  public UserAccount requireUser(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new IllegalStateException("Authenticated user not found");
    }
    return userAccountRepository
        .findByUsername(authentication.getName())
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
  }
}
