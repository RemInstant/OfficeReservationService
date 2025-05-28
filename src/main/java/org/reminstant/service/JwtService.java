package org.reminstant.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
  private final SecretKey jwtSigningKey;
  private final JwtParser jwtParser;

  public JwtService(@Value("${token.signing.key}") String jwtSigningKey) {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
    this.jwtSigningKey = Keys.hmacShaKeyFor(keyBytes);
    this.jwtParser = Jwts.parser().verifyWith(this.jwtSigningKey).build();
  }

  public String generateToken(String username, Duration ttl) {
    return generateToken(Collections.emptyMap(), username, ttl);
  }

  public String generateToken(Map<String, Object> extraClaims, String username, Duration ttl) {
    Instant now = Instant.now();
    return Jwts.builder()
        .claims(extraClaims)
        .subject(username)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(jwtSigningKey)
        .compact();
  }

  public boolean isTokenValid(String token) {
    try {
      jwtParser.parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String extractUsername(String token) {
    return jwtParser.parseSignedClaims(token).getPayload().getSubject();
  }
}