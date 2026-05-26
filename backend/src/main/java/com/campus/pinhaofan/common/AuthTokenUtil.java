package com.campus.pinhaofan.common;

import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class AuthTokenUtil {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final long expireSeconds;

    public AuthTokenUtil(
            @Value("${auth.token.secret}") String secret,
            @Value("${auth.token.expire-hours}") long expireHours) {
        this.secret = secret;
        this.expireSeconds = expireHours * 3600;
    }

    public String createToken(Long userId) {
        long expiresAt = Instant.now().getEpochSecond() + expireSeconds;
        String payload = encode(userId + "|" + expiresAt);
        return payload + "." + sign(payload);
    }

    public Long parseUserIdFromAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }
        return parseUserId(authorization.substring(TOKEN_PREFIX.length()).trim());
    }

    public Long parseUserId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }

        String[] payload = decode(parts[0]).split("\\|");
        if (payload.length != 2) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(payload[1]);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }

        try {
            return Long.valueOf(payload[0]);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign auth token", exception);
        }
    }

    private String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
