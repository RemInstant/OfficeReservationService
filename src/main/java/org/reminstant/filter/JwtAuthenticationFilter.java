package org.reminstant.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.reminstant.service.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String AUTHORIZATION_PREFIX = "Bearer ";
  private static final String ROLE_PREFIX = "ROLE_";

  private final JwtService jwtService;
  private final AuthenticationEntryPoint entryPoint;
  private final AntPathMatcher pathMatcher;
  private final Set<String> skipUris;

  public JwtAuthenticationFilter(JwtService jwtService, AuthenticationEntryPoint entryPoint) {
    this.jwtService = jwtService;
    this.entryPoint = entryPoint;
    this.pathMatcher = new AntPathMatcher();
    this.skipUris = new HashSet<>();
  }

  public void addSkipUri(String url) {
    skipUris.add(url);
  }

  public void addSkipUris(String ...urls) {
    skipUris.addAll(List.of(urls));
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (skipUris.stream().anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()))) {
      filterChain.doFilter(request, response);
      return;
    }

    String tokenBearer = request.getHeader(AUTHORIZATION_HEADER);

    if (tokenBearer != null && tokenBearer.startsWith(AUTHORIZATION_PREFIX)) {
      String token = tokenBearer.substring(AUTHORIZATION_PREFIX.length());

      if (jwtService.isAccessTokenInvalid(token)) {
        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials"));
        return;
      }

      String username;
      String authority;
      try {
        username = jwtService.extractUsername(token);
        authority = ROLE_PREFIX + jwtService.extractRole(token);
      } catch (JwtException ex) {
        entryPoint.commence(request, response, new BadCredentialsException("Bad credentials", ex));
        return;
      }

      PreAuthenticatedAuthenticationToken authToken = new PreAuthenticatedAuthenticationToken(
          username, token, List.of(new SimpleGrantedAuthority(authority)));
      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);
  }
}
