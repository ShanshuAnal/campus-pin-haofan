package com.campus.pinhaofan.common;

import com.campus.pinhaofan.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisConcurrencyGuardTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisConcurrencyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new RedisConcurrencyGuard(stringRedisTemplate, new TokenHashUtil());
    }

    @Test
    void runIdempotentSkipsRedisWhenKeyIsBlank() {
        String result = guard.runIdempotent("group-order:create", "Bearer token", " ", () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void runIdempotentRejectsDuplicateSubmission() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> guard.runIdempotent(
                "group-order:create",
                "Bearer token",
                "req-1",
                () -> "ok"
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("重复提交，请勿重复操作");
        });
    }

    @Test
    void runIdempotentDeletesKeyWhenBusinessFails() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        assertThatThrownBy(() -> guard.runIdempotent(
                "group-order:create",
                "Bearer token",
                "req-2",
                () -> {
                    throw new BusinessException(400, "bad request");
                }
        )).isInstanceOf(BusinessException.class);

        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    void runWithOrderWriteLockRejectsConcurrentOperation() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("group-order:lock:write:2001"), anyString(), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> guard.runWithOrderWriteLock(2001L, () -> "ok"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(409);
                    assertThat(exception.getMessage()).isEqualTo("拼单操作处理中，请稍后重试");
                });
    }

    @Test
    void runWithOrderWriteLockReleasesOwnLockAfterSuccess() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<String> lockToken = new AtomicReference<>();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("group-order:lock:write:2001"), anyString(), any(Duration.class)))
                .thenAnswer(invocation -> {
                    lockToken.set(invocation.getArgument(1));
                    return true;
                });
        when(valueOperations.get("group-order:lock:write:2001")).thenAnswer(invocation -> lockToken.get());

        String result = guard.runWithOrderWriteLock(2001L, () -> {
            executed.set(true);
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(executed).isTrue();
        verify(stringRedisTemplate).delete("group-order:lock:write:2001");
    }
}
