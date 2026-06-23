package com.ticket.security;

import com.ticket.common.constant.CacheConstants;
import com.ticket.common.exception.TokenBlacklistedException;
import com.ticket.common.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final RedissonClient redissonClient;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            RedissonClient redissonClient) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                Base64.getEncoder().encodeToString(secret.getBytes())));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redissonClient = redissonClient;
    }

    public String generateAccessToken(Long userId, String role, Long tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .claim("role", role)
                .claim("tenant_id", tenantId) // null = superadmin (claim omitted)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();

        // Store in Redis whitelist
        String redisKey = CacheConstants.REFRESH_TOKEN_PREFIX + userId + ":" + jti;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set(token, Duration.ofMillis(refreshTokenExpiration));

        return token;
    }

    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check blacklist by JWT ID (jti) — both access and refresh tokens carry a jti
            String jti = claims.getId();
            if (jti != null) {
                String blacklistKey = CacheConstants.BLACKLIST_PREFIX + jti;
                RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
                if (bucket.isExists()) {
                    throw new TokenBlacklistedException();
                }
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | TokenBlacklistedException e) {
            if (e instanceof TokenBlacklistedException) throw (TokenBlacklistedException) e;
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public void blacklistAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0 && claims.getId() != null) {
                String blacklistKey = CacheConstants.BLACKLIST_PREFIX + claims.getId();
                RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
                bucket.set("revoked", Duration.ofMillis(remaining));
            }
        } catch (JwtException ignored) {}
    }

    public void revokeRefreshToken(Long userId, String jti) {
        String redisKey = CacheConstants.REFRESH_TOKEN_PREFIX + userId + ":" + jti;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.delete();
    }

    public boolean isRefreshTokenValid(Long userId, String jti) {
        String redisKey = CacheConstants.REFRESH_TOKEN_PREFIX + userId + ":" + jti;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        return bucket.isExists();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;
        return Long.parseLong(claims.getSubject());
    }
}
