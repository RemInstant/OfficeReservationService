package org.reminstant.config;

import org.reminstant.filter.CustomAuthenticationEntryPoint;
import org.reminstant.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity httpSecurityBuilder,
                                          JwtAuthenticationFilter jwtAuthenticationFilter,
                                          @Value("${api.sign-up}") String signUpEndpoint,
                                          @Value("${api.sign-in}") String signInEndpoint)
      throws Exception {
    return httpSecurityBuilder
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(c -> c
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(c -> c
            .requestMatchers(HttpMethod.POST, signUpEndpoint).permitAll()
            .requestMatchers(HttpMethod.POST, signInEndpoint).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/test-redis").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/test-mongo").permitAll()
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

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint() {
    CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
    entryPoint.setRealmName("default");
    return entryPoint;
  }

  @Bean
  BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
