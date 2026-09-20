package com.TicketMaster.user_service.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login-attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Increments the failure counter for the given email.
     * Sets a 15-minute TTL on the first failure.
     */
    public void recordFailure(String email) {
        String key = KEY_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // First attempt recorded — set the expiry window
            redisTemplate.expire(key, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * Returns true if the account is locked (>= 5 failed attempts within the window).
     */
    public boolean isBlocked(String email) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= MAX_ATTEMPTS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Resets the failure counter for the given email (called after successful login).
     */
    public void resetAttempts(String email) {
        redisTemplate.delete(KEY_PREFIX + email);
    }
}
