package org.superwindcloud.webstore.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.superwindcloud.webstore.config.SecurityProperties;

@Service
public class JwtService {

  private final SecurityProperties securityProperties;

  public JwtService(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("scope", userDetails.getAuthorities().stream().map(Object::toString).toList());
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(securityProperties.jwtExpirationHours() * 3600);

    return Jwts.builder()
        .claims(claims)
        .subject(userDetails.getUsername())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(getSigningKey())
        .compact();
  }

  public String extractUsername(String token) {
    try {
      return extractAllClaims(token).getSubject();
    } catch (RuntimeException ex) {
      return null;
    }
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token);
    return username != null
        && username.equals(userDetails.getUsername())
        && !extractAllClaims(token).getExpiration().before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSigningKey() {
    String secret = securityProperties.jwtSecret();
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(secret);
    } catch (IllegalArgumentException ex) {
      keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
