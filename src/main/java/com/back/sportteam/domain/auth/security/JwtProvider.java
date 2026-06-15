package com.back.sportteam.domain.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-seconds}") long accessTokenExpiry,
            @Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenExpiry
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry * 1000;
        this.refreshTokenExpiry = refreshTokenExpiry * 1000;
    }

    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, accessTokenExpiry);
    }

    public String generateRefreshToken(Long userId, String role) {
        return buildToken(userId, role, refreshTokenExpiry);
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException _) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private String buildToken(Long userId, String role, long expiry) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiry)))
                .signWith(secretKey)
                .compact();
    }
}