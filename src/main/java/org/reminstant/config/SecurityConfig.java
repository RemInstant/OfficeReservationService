package org.reminstant.config;

import org.reminstant.filter.CustomAuthenticationEntryPoint;
import org.reminstant.filter.JwtAuthenticationFilter;
import org.reminstant.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

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
  @Value("${api.credentials.refresh-token}")
  private String refreshTokenUri;
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
            .requestMatchers(HttpMethod.POST, refreshTokenUri).permitAll()
            .requestMatchers(managementUriMask).hasRole("ADMIN")
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, BasicAuthenticationFilter.class)
        .build();
  }

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                  AuthenticationEntryPoint entryPoint) {
    var filter = new JwtAuthenticationFilter(jwtService, entryPoint);
    filter.addSkipUris(swaggerUrls);
    filter.addSkipUris(signUpUri);
    filter.addSkipUri(signInUri);
    filter.addSkipUri(refreshTokenUri);
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
