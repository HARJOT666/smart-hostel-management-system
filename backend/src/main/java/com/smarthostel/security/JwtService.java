package com.smarthostel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and checks JSON Web Tokens.
 * A token contains: subject (username), uid, role, name, issued-at and expiry,
 * and is signed with HMAC-SHA256 using the secret from application.yml.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(AppUserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(getExpiry(now)))
                .signWith(key)
                .compact();
    }

    public Instant getExpiry(Instant issuedAt) {
        return issuedAt.plusSeconds(expirationMinutes * 60);
    }

    /**
     * Verifies the signature and expiry, then rebuilds the user from the claims.
     * Returns null if the token is invalid or expired.
     */
    public AppUserDetails parseToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new AppUserDetails(
                    claims.get("uid", Long.class),
                    claims.getSubject(),
                    null,
                    claims.get("role", String.class),
                    claims.get("name", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
