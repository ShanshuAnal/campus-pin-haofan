package com.campus.pinhaofan.common;

import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class RedisConcurrencyGuard {

    public static final String IDEMPOTENCY_PREFIX = "group-order:idempotency:";
    public static final String WRITE_LOCK_PREFIX = "group-order:lock:write:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(10);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final TokenHashUtil tokenHashUtil;

    public RedisConcurrencyGuard(StringRedisTemplate stringRedisTemplate, TokenHashUtil tokenHashUtil) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenHashUtil = tokenHashUtil;
    }

    public <T> T runIdempotent(
            String operation,
            String authorization,
            String idempotencyKey,
            Supplier<T> supplier) {
        String key = buildIdempotencyKey(operation, authorization, idempotencyKey);
        if (key == null) {
            return supplier.get();
        }

        acquireIdempotencyKey(key);
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            deleteQuietly(key);
            throw exception;
        }
    }

    public <T> T runWithOrderWriteLock(Long orderId, Supplier<T> supplier) {
        String key = buildOrderWriteLockKey(orderId);
        String token = UUID.randomUUID().toString();
        acquireLock(key, token);
        try {
            return supplier.get();
        } finally {
            releaseLock(key, token);
        }
    }

    public <T> T runIdempotentWithOrderWriteLock(
            String operation,
            String authorization,
            String idempotencyKey,
            Long orderId,
            Supplier<T> supplier) {
        return runIdempotent(operation, authorization, idempotencyKey,
                () -> runWithOrderWriteLock(orderId, supplier));
    }

    public String buildIdempotencyKey(String operation, String authorization, String idempotencyKey) {
        String normalizedKey = normalize(idempotencyKey);
        if (normalizedKey.isEmpty()) {
            return null;
        }
        String principalHash = tokenHashUtil.sha256(normalize(authorization));
        String requestHash = tokenHashUtil.sha256(normalizedKey);
        return IDEMPOTENCY_PREFIX + normalize(operation) + ":" + principalHash + ":" + requestHash;
    }

    public String buildOrderWriteLockKey(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "orderId 不合法");
        }
        return WRITE_LOCK_PREFIX + orderId;
    }

    private void acquireIdempotencyKey(String key) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    key,
                    String.valueOf(Instant.now().toEpochMilli()),
                    IDEMPOTENCY_TTL
            );
            if (!Boolean.TRUE.equals(acquired)) {
                throw new BusinessException(ResultCode.CONFLICT.getCode(), "重复提交，请勿重复操作");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "Redis 防重复提交不可用");
        }
    }

    private void acquireLock(String key, String token) {
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
            if (!Boolean.TRUE.equals(acquired)) {
                throw new BusinessException(ResultCode.CONFLICT.getCode(), "拼单操作处理中，请稍后重试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "Redis 短时锁不可用");
        }
    }

    private void releaseLock(String key, String token) {
        try {
            String currentToken = stringRedisTemplate.opsForValue().get(key);
            if (token.equals(currentToken)) {
                stringRedisTemplate.delete(key);
            }
        } catch (RuntimeException exception) {
            // The lock has a short TTL, so a release failure must not mask a committed business result.
        }
    }

    private void deleteQuietly(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException ignored) {
            // The original business exception should remain the visible failure.
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
