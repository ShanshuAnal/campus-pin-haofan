package com.campus.pinhaofan.common;

import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AccessTokenBlacklist accessTokenBlacklist;

    @Autowired
    public AuthTokenUtil(
            @Value("${auth.token.secret}") String secret,
            @Value("${auth.token.expire-hours}") long expireHours,
            AccessTokenBlacklist accessTokenBlacklist) {
        this.secret = secret;
        this.expireSeconds = expireHours * 3600;
        this.accessTokenBlacklist = accessTokenBlacklist;
    }

    public AuthTokenUtil(String secret, long expireHours) {
        this.secret = secret;
        this.expireSeconds = expireHours * 3600;
        this.accessTokenBlacklist = null;
    }

    public String createToken(Long userId) {
        long expiresAt = Instant.now().getEpochSecond() + expireSeconds;
        String payload = encode(userId + "|" + expiresAt);
        return payload + "." + sign(payload);
    }

    public Long parseUserIdFromAuthorization(String authorization) {
        String token = extractBearerToken(authorization);
        if (accessTokenBlacklist != null && accessTokenBlacklist.contains(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
        return parseUserId(token);
    }

    public Long parseUserId(String token) {
        validateTokenFormat(token);
        String[] payload = decodePayload(token);
        validateExpiresAt(payload[1]);
        try {
            return Long.valueOf(payload[0]);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
    }

    public long parseExpiresAt(String token) {
        validateTokenFormat(token);
        String[] payload = decodePayload(token);
        return validateExpiresAt(payload[1]);
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }
        String token = authorization.substring(TOKEN_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }
        return token;
    }

    private void validateTokenFormat(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
    }

    private String[] decodePayload(String token) {
        String[] parts = token.split("\\.");
        String[] payload = decode(parts[0]).split("\\|");
        if (payload.length != 2) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
        return payload;
    }

    private long validateExpiresAt(String expiresAtValue) {
        long expiresAt;
        try {
            expiresAt = Long.parseLong(expiresAtValue);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已失效");
        }
        return expiresAt;
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
