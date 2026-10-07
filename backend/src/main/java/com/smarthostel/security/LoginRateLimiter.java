package com.smarthostel.security;

import com.smarthostel.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Simple Redis-based limit on failed logins.
 *
 * Redis key  : rate:login:fail:<ip address>
 * Redis value: number of failed attempts
 * Expiry     : the key is created with a 5-minute expiry, so the counter
 *              disappears by itself 5 minutes after the first failure.
 *
 * When the counter reaches the limit (5), further login requests from
 * that IP get HTTP 429 until the key expires. A successful login deletes the key.
 *
 * If Redis is not running, logins are still allowed (the limiter "fails open")
 * because Oracle - not Redis - is what the application really depends on.
 */
@Slf4j
@Component
public class LoginRateLimiter {

    private static final String KEY_PREFIX = "rate:login:fail:";

    private final StringRedisTemplate redis;
    private final int maxFailures;
    private final Duration window;

    public LoginRateLimiter(StringRedisTemplate redis,
                            @Value("${app.rate-limit.login-max-failures}") int maxFailures,
                            @Value("${app.rate-limit.login-window-seconds}") long windowSeconds) {
        this.redis = redis;
        this.maxFailures = maxFailures;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** Throws RateLimitExceededException (HTTP 429) if this IP already has too many failures. */
    public void checkAllowed(String ip) {
        try {
            String value = redis.opsForValue().get(KEY_PREFIX + ip);
            if (value != null && Integer.parseInt(value) >= maxFailures) {
                Long secondsLeft = redis.getExpire(KEY_PREFIX + ip);
                throw new RateLimitExceededException(secondsLeft == null || secondsLeft < 0 ? 1 : secondsLeft);
            }
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, login rate limit not checked: {}", e.getMessage());
        }
    }

    /** Adds one failed attempt. The first failure also starts the 5-minute expiry. */
    public void recordFailure(String ip) {
        try {
            Long count = redis.opsForValue().increment(KEY_PREFIX + ip);
            if (count != null && count == 1) {
                redis.expire(KEY_PREFIX + ip, window);
            }
            log.info("Failed login from {} (attempt {} of {})", ip, count, maxFailures);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, failed login not counted: {}", e.getMessage());
        }
    }

    /** Clears the counter after a successful login. */
    public void reset(String ip) {
        try {
            redis.delete(KEY_PREFIX + ip);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable, could not reset login counter: {}", e.getMessage());
        }
    }
}
