package org.reminstant.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.reminstant.exception.InvalidCredentialsException;
import org.reminstant.exception.OccupiedUsernameException;
import org.reminstant.model.AppUser;
import org.reminstant.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class AppUserService implements UserDetailsService {

  private final AppUserRepository appUserRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  @Value("${admin-user-details.username}")
  private String adminUsername;
  @Value("${admin-user-details.password}")
  private String adminPassword;

  public AppUserService(AppUserRepository appUserRepository,
                        PasswordEncoder passwordEncoder,
                        JdbcTemplate jdbcTemplate) {
    this.appUserRepository = appUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  @Transactional
  public void initAdminUser() {
    String sql = "INSERT INTO app_user (username, password, role) VALUES (?, ?, ?)";
    String encodedPassword = passwordEncoder.encode(adminPassword);

    try {
      jdbcTemplate.update(sql, adminUsername, encodedPassword, "ADMIN");
    } catch (Exception ex) {
      // already registered
    }
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
  public void registerUser(String username, String password, String role)
      throws InvalidCredentialsException, OccupiedUsernameException {
    if (username == null) {
      throw new InvalidCredentialsException("Username cannot be null");
    }
    if (password == null) {
      throw new InvalidCredentialsException("Password cannot be null");
    }
    if (appUserRepository.existsAppUserByUsername(username)) {
      throw new OccupiedUsernameException("Username is occupied");
    }

    String encodedPassword = passwordEncoder.encode(password);
    appUserRepository.save(new AppUser(username, encodedPassword, role));
  }

  public boolean verifyUser(AppUser user, String password) {
    if (user == null || password == null) {
      return false;
    }

    return passwordEncoder.matches(password, user.getPassword());
  }

  public AppUser getUser(String username) throws UsernameNotFoundException {
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