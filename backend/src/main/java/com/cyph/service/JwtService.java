package com.cyph.service;

import com.cyph.config.CyphProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Arrays;

/**
 * Issues and validates JWTs for extension auth. Token subject is user email.
 */
@Service
public class JwtService {

    private final CyphProperties cyphProperties;

    public JwtService(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
    }

    private SecretKey getSigningKey() {
        byte[] raw = cyphProperties.getAuth().getExtensionJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = raw.length >= 32 ? raw : Arrays.copyOf(raw, 32);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueToken(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email required");
        int expMinutes = cyphProperties.getAuth().getExtensionJwt().getExpirationMinutes();
        SecretKey key = getSigningKey();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expMinutes * 60L);
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /** Returns email (subject) if valid; null if missing/invalid/expired. */
    public String parseEmailFromToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) return null;
        String token = bearerToken.substring(7).trim();
        if (token.isEmpty()) return null;
        try {
            SecretKey key = getSigningKey();
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
