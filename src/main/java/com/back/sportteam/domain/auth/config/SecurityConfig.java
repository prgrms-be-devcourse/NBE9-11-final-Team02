package com.back.sportteam.domain.auth.config;

import com.back.sportteam.domain.auth.security.JwtAuthenticationFilter;
import com.back.sportteam.domain.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            return http
                    .csrf(csrf -> csrf
                            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                            .ignoringRequestMatchers(
                                    "/api/v1/auth/**",
                                    "/api/v1/matches/**",
                                    "/api/v1/facilities/**",
                                    "/api/v1/users/**",
                                    "/api/v1/payments/**",
                                    "/api/v1/queue/**",
                                    "/api/v1/manager/**",
                                    "/api/v1/admin/**",
                                    "/api/v1/health",
                                    "/ws/**"
                            )
                    )
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/api/v1/auth/signup",
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/refresh",
                                    "/api/v1/payments/webhook/**",
                                    "/api/v1/health",
                                    "/actuator/health",
                                    "/actuator/prometheus",
                                    "/api/v1/facilities/**"
                            ).permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/v1/matches").permitAll()
                            .requestMatchers(RegexRequestMatcher.regexMatcher(
                                    HttpMethod.GET,
                                    "^/api/v1/matches/[0-9a-fA-F-]{36}$"
                            )).permitAll()
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/v1/manager/**").hasRole("MANAGER")
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(
                            new JwtAuthenticationFilter(jwtProvider, redisTemplate),
                            UsernamePasswordAuthenticationFilter.class
                    )
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure security filter chain.", e);
        }
    }
}
