package com.campus.pinhaofan.common;

import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AccessTokenBlacklist {

    public static final String ACCESS_BLACKLIST_PREFIX = "auth:blacklist:access:";

    private final TokenHashUtil tokenHashUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public AccessTokenBlacklist(TokenHashUtil tokenHashUtil, StringRedisTemplate stringRedisTemplate) {
        this.tokenHashUtil = tokenHashUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void blacklist(String accessToken, long expiresAtEpochSecond) {
        long now = Instant.now().getEpochSecond();
        long ttlSeconds = expiresAtEpochSecond - now;
        if (ttlSeconds <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }

        try {
            stringRedisTemplate.opsForValue().set(
                    buildKey(accessToken),
                    String.valueOf(now),
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (RuntimeException exception) {
            throw redisUnavailable();
        }
    }

    public boolean contains(String accessToken) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(accessToken)));
        } catch (RuntimeException exception) {
            throw redisUnavailable();
        }
    }

    public String buildKey(String accessToken) {
        return ACCESS_BLACKLIST_PREFIX + tokenHashUtil.sha256(accessToken);
    }

    private BusinessException redisUnavailable() {
        return new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "Redis 黑名单不可用");
    }
}
