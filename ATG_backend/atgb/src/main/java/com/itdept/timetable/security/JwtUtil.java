package com.itdept.timetable.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "yourSecretKeyForJwt2026!@#yourSecretKeyForJwt2026!@#";
    private final long EXPIRATION = 86400000; // 1 day

    private SecretKey getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        // ✅ Add null check
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is null or empty");
        }
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        // ✅ Handle null/empty token gracefully
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        // ✅ Handle null/empty token gracefully
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            System.out.println("Failed to extract email: " + e.getMessage());
            return null;
        }
    }

    public String extractRole(String token) {
        // ✅ Handle null/empty token gracefully
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return (String) extractAllClaims(token).get("role");
        } catch (Exception e) {
            System.out.println("Failed to extract role: " + e.getMessage());
            return null;
        }
    }
}