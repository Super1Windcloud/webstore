package org.superwindcloud.webstore.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.repository.UserAccountRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

  private final UserAccountRepository userAccountRepository;

  public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userAccountRepository
        .findByUsername(username)
        .map(
            user ->
                User.withUsername(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
