package com.ticket.common.constant;

/**
 * Redis key prefixes and cache-related constants.
 * All Redis keys follow the pattern: {@code prefix:param1:param2}.
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /**
     * Refresh token whitelist key prefix.
     * Pattern: {@code refresh:{userId}:{jti}}
     * Stored value: the raw refresh token string.
     * TTL matches the refresh token expiration (7 days).
     */
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";

    /**
     * Access token blacklist key prefix.
     * Pattern: {@code blacklist:{tokenHash}}
     * Stored value: "revoked".
     * TTL = remaining lifetime of the original access token.
     */
    public static final String BLACKLIST_PREFIX = "blacklist:";
}
