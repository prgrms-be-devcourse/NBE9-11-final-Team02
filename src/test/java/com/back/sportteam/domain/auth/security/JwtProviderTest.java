package com.back.sportteam.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "test-secret-key-must-be-at-least-32-characters-long",
                1800L,
                1209600L
        );
    }

    @DisplayName("액세스 토큰 생성")
    @Test
    void 액세스_토큰_생성() {
        String token = jwtProvider.generateAccessToken(UUID.randomUUID(), "USER");

        assertThat(token).isNotBlank();
    }

    @DisplayName("리프레시 토큰 생성")
    @Test
    void 리프레시_토큰_생성() {
        String token = jwtProvider.generateRefreshToken(UUID.randomUUID(), "USER");

        assertThat(token).isNotBlank();
    }

    @DisplayName("유효한 토큰 파싱 시 클레임 반환")
    @Test
    void 유효한_토큰_파싱_시_클레임_반환() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(userId, "USER");

        Claims claims = jwtProvider.parse(token);

        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @DisplayName("유효한 토큰일 시 true를 반환")
    @Test
    void 유효한_토큰일_시_true를_반환() {
        String token = jwtProvider.generateAccessToken(UUID.randomUUID(), "USER");

        assertThat(jwtProvider.isValid(token)).isTrue();
    }

    @DisplayName("유효하지 않은 토큰일 시 false를 반환")
    @Test
    void 유효하지_않은_토큰일_시_false를_반환() {
        assertThat(jwtProvider.isValid("invalid.token.value")).isFalse();
    }

    @DisplayName("유효하지 않은 토큰 파싱 시 예외 발생")
    @Test
    void 유효하지_않은_토큰_파싱_시_예외_발생() {
        assertThatThrownBy(() -> jwtProvider.parse("invalid.token.value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }
}