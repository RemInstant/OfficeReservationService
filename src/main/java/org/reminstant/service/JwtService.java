package org.reminstant.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.reminstant.model.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

  private final AppUserService appUserService;
  private final StringRedisTemplate redisTemplate;

  private final SecretKey jwtSigningKey;
  private final JwtParser jwtParser;

  public JwtService(AppUserService appUserService,
                    StringRedisTemplate redisTemplate,
                    @Value("${token.signing.key}") String jwtSigningKey) {
    this.appUserService = appUserService;
    this.redisTemplate = redisTemplate;

    byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
    this.jwtSigningKey = Keys.hmacShaKeyFor(keyBytes);
    this.jwtParser = Jwts.parser().verifyWith(this.jwtSigningKey).build();
  }

  public String generateAccessToken(AppUser user, Duration ttl) {
    Map<String, Object> claims = Map.of("Role", user.getRole());
    return generateToken(claims, user.getUsername(), ttl);
  }

  public String generateAccessToken(String username, String role, Duration ttl) {
    Map<String, Object> claims = Map.of("Role", role);
    return generateToken(claims, username, ttl);
  }

  public String generateRefreshToken(AppUser user, Duration ttl) {
    long tokenVersion = appUserService.incrementUserTokenVersion(user.getUsername());
    Map<String, Object> claims = Map.of("Role", user.getRole(), "Version", tokenVersion);
    return generateToken(claims, user.getUsername(), ttl);
  }

  public String generateRefreshToken(String username, String role, Duration ttl) {
    long tokenVersion = appUserService.incrementUserTokenVersion(username);
    Map<String, Object> claims = Map.of("Role", role, "Version", tokenVersion);
    return generateToken(claims, username, ttl);
  }

  public boolean isAccessTokenInvalid(String token) {
    try {
      jwtParser.parseSignedClaims(token);
      if (isTokenBlacklisted(token)) {
        return true;
      }
      Long version = extractVersion(token);
      return version != null;
    } catch (Exception e) {
      return true;
    }
  }

  public boolean isRefreshTokenInvalid(String token) {
    try {
      jwtParser.parseSignedClaims(token);
      if (isTokenBlacklisted(token)) {
        return true;
      }
      Long version = extractVersion(token);
      if (version != null) {
        AppUser user = appUserService.getUser(extractUsername(token));
        return !user.getTokenVersion().equals(version);
      }
      return true;
    } catch (Exception e) {
      return true;
    }
  }

  public String extractUsername(String token) {
    return jwtParser.parseSignedClaims(token).getPayload().getSubject();
  }

  public String extractRole(String token) {
    return jwtParser.parseSignedClaims(token).getPayload().get("Role", String.class);
  }

  public void blacklistToken(String token, Duration ttl) {
    redisTemplate.opsForValue().set(token, "", ttl);
  }

  public boolean isTokenBlacklisted(String token) {
    return redisTemplate.opsForValue().get(token) != null;
  }

  private String generateToken(Map<String, Object> extraClaims, String username, Duration ttl) {
    Instant now = Instant.now();
    return Jwts.builder()
        .claims(extraClaims)
        .subject(username)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(jwtSigningKey)
        .compact();
  }

  public Long extractVersion(String token) {
    try {
      return jwtParser.parseSignedClaims(token).getPayload().get("Version", Long.class);
    } catch (Exception ex) {
      return null;
    }
  }
}