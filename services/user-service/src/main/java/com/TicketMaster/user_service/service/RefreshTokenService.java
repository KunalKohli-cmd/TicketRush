package com.TicketMaster.user_service.service;

import com.TicketMaster.user_service.config.JwtProperties;
import com.TicketMaster.user_service.exceptions.TokenException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties props;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties props) {
        this.redisTemplate = redisTemplate;
        this.props = props;
    }

    /**
     * Generates a new refresh token, stores userId in Redis with TTL, and returns the token.
     */
    public String create(Long userId) {
        String token = UUID.randomUUID().toString();
        long ttlSeconds = props.refreshTokenTtl().getSeconds();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), ttlSeconds, TimeUnit.SECONDS);
        return token;
    }

    /**
     * Validates the refresh token. Returns the associated userId, or throws TokenException if absent/expired.
     */
    public Long validate(String token) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            throw new TokenException("Refresh token invalid or expired");
        }
        return Long.parseLong(value);
    }

    /**
     * Revokes a single refresh token by deleting it from Redis.
     */
    public void revoke(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }

    /**
     * TODO: Revoke all refresh tokens for a given user (requires user-keyed index in Redis).
     * Not implemented in MVP.
     */
    public void revokeAllForUser(Long userId) {
        // TODO: implement with a user-to-tokens index (e.g. Redis Set)
    }
}
