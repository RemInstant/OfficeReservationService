package org.reminstant.service;

import org.reminstant.exception.InvalidCredentialsException;
import org.reminstant.exception.OccupiedUsernameException;
import org.reminstant.model.AppUser;
import org.reminstant.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AppUserService implements UserDetailsService {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${admin-user-details.username}")
  String adminUsername;
  @Value("${admin-user-details.password}")
  String adminPassword;


  public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (username.equals(adminUsername)) {
      return User.builder()
          .username(adminUsername)
          .password(adminPassword)
          .roles("ADMIN")
          .build();
    }

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
    if (username == null) {
      throw new InvalidCredentialsException("Username cannot be null");
    }
    if (password == null) {
      throw new InvalidCredentialsException("Password cannot be null");
    }
    if (appUserRepository.existsAppUserByUsername(username) || username.equals(adminUsername)) {
      throw new OccupiedUsernameException("Username is occupied");
    }

    String encryptedPassword = passwordEncoder.encode(password);
    appUserRepository.save(new AppUser(username, encryptedPassword));
  }

  public boolean verifyUser(AppUser user, String password) {
    if (user == null || password == null) {
      return false;
    }

    if (user.getUsername().equals(adminUsername)) {
      return password.equals(adminPassword);
    }

    return passwordEncoder.matches(password, user.getPassword());
  }

  public AppUser getUser(String username) throws UsernameNotFoundException {
    if (username.equals(adminUsername)) {
      return new AppUser(adminUsername, adminPassword, "ADMIN");
    }
    return appUserRepository
        .getAppUserByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("No user with username '%s'".formatted(username)));
  }

  @Transactional
  public long incrementUserTokenVersion(String username) throws UsernameNotFoundException {
    AppUser user = getUser(username);
    Long tokenVersion = user.getTokenVersion();
    if (tokenVersion == null) {
      tokenVersion = 0L;
    }
    user.setTokenVersion(tokenVersion + 1);
    appUserRepository.save(user);
    return user.getTokenVersion();
  }
}