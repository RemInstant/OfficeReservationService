package org.reminstant.service;

import org.reminstant.exception.InvalidCredentialsException;
import org.reminstant.exception.OccupiedUsernameException;
import org.reminstant.model.AppUser;
import org.reminstant.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AppUserService implements UserDetailsService {

  private final AppUserRepository appUserRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public AppUserService(AppUserRepository appUserRepository, BCryptPasswordEncoder passwordEncoder) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<AppUser> user = appUserRepository.getAppUserByUsername(username);

    if (user.isEmpty()) {
      throw new UsernameNotFoundException("username '%s' not found".formatted(username));
    }

    return User.builder()
        .username(user.get().getUsername())
        .password(user.get().getPassword())
        .roles(user.get().getRole())
        .build();
  }

  @Transactional
  public void registerUser(String username, String password)
      throws InvalidCredentialsException, OccupiedUsernameException {
    if (username == null || username.length() < 4 ||
        username.length() > 16 || !username.matches("\\w+")) {
      throw new InvalidCredentialsException("Invalid username");
    }
    if (password == null || password.length() < 6) {
      throw new InvalidCredentialsException("Invalid password");
    }
    if (appUserRepository.existsAppUserByUsername(username)) {
      throw new OccupiedUsernameException("Username is occupied");
    }

    String encryptedPassword = passwordEncoder.encode(password);
    appUserRepository.save(new AppUser(username, encryptedPassword));
  }

  public boolean verifyUser(String username, String password) {
    if (username == null || password == null) {
      return false;
    }

    return appUserRepository
        .getAppUserByUsername(username)
        .filter(user -> passwordEncoder.matches(password, user.getPassword()))
        .isPresent();
  }

  public AppUser getUser(String username) {
    return appUserRepository.getAppUserByUsername(username).orElse(null);
  }
}