package com.ticket.security;

import com.ticket.common.exception.TokenBlacklistedException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);

        RBucket<String> mockBucket = mock(RBucket.class);
        when(mockBucket.isExists()).thenReturn(true);
        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);

        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-testing-only",
                7200000L, 604800000L, redissonClient);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_USER");
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.validateToken(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role")).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken(1L);
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.validateToken(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        Claims claims = jwtTokenProvider.validateToken("invalid.token.here");
        assertThat(claims).isNull();
    }

    @Test
    void shouldReturnUserIdFromToken() {
        String token = jwtTokenProvider.generateAccessToken(42L, "ROLE_AGENT");
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void shouldValidateRefreshTokenInWhitelist() {
        String token = jwtTokenProvider.generateRefreshToken(1L);
        Claims claims = jwtTokenProvider.validateToken(token);
        boolean valid = jwtTokenProvider.isRefreshTokenValid(1L, claims.getId());
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRevokeRefreshToken() {
        jwtTokenProvider.revokeRefreshToken(1L, "some-jti");
        // Should not throw — revocation is best-effort via delete
    }

    @Test
    void shouldBlacklistAccessToken() {
        RBucket<String> mockBucket = mock(RBucket.class);
        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);

        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_USER");
        jwtTokenProvider.blacklistAccessToken(token);
        // Should not throw — blacklisting sets TTL
    }
}
