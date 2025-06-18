package org.reminstant.config;

import org.reminstant.filter.CustomAuthenticationEntryPoint;
import org.reminstant.filter.JwtAuthenticationFilter;
import org.reminstant.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

  private final String[] swaggerUrls = {
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html"
  };
  @Value("${api.credentials.sign-up}")
  private String signUpUri;
  @Value("${api.credentials.sign-in}")
  private String signInUri;
  @Value("${api.management.mask}")
  private String managementUriMask;


  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity httpSecurityBuilder,
                                          JwtAuthenticationFilter jwtAuthenticationFilter)
      throws Exception {
    return httpSecurityBuilder
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(c -> c
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(c -> c
            .requestMatchers(HttpMethod.GET, swaggerUrls).permitAll()
            .requestMatchers(HttpMethod.POST, signUpUri).permitAll()
            .requestMatchers(HttpMethod.POST, signInUri).permitAll()
            .requestMatchers(managementUriMask).hasRole("ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class)
        .build();

//    DisableEncodeUrlFilter,
//    WebAsyncManagerIntegrationFilter,
//    SecurityContextHolderFilter,
//    HeaderWriterFilter,
//    LogoutFilter,
//    JwtAuthenticationFilter,
//    RequestCacheAwareFilter,
//    SecurityContextHolderAwareRequestFilter,
//    AnonymousAuthenticationFilter,
//    SessionManagementFilter,
//    ExceptionTranslationFilter,
//    AuthorizationFilter
  }

//  @Bean
//  AuthenticationManager authManager(PasswordEncoder passwordEncoder,
//                                    UserDetailsService userDetailsService,
//                                    @Value("${admin-user-details.username}") String adminUsername,
//                                    @Value("${admin-user-details.password}") String adminPassword) {
//    DaoAuthenticationProvider mainProvider = new DaoAuthenticationProvider(passwordEncoder);
//    mainProvider.setUserDetailsService(userDetailsService);
//
//    UserDetails adminUserDetails = new User(
//        adminUsername, adminPassword, List.of(new SimpleGrantedAuthority("ADMIN")));
//    DaoAuthenticationProvider adminProvider = new DaoAuthenticationProvider(passwordEncoder);
//    adminProvider.setUserDetailsService(new InMemoryUserDetailsManager(adminUserDetails));
//
//    return new ProviderManager(mainProvider, adminProvider);
//  }

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                  AuthenticationEntryPoint entryPoint) {
    var filter = new JwtAuthenticationFilter(jwtService, entryPoint);
    filter.addSkipUris(swaggerUrls);
    filter.addSkipUris(signUpUri);
    filter.addSkipUri(signInUri);
    return filter;
  }

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint() {
    return new CustomAuthenticationEntryPoint();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
