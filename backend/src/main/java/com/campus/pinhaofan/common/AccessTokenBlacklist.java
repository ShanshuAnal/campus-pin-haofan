package com.campus.pinhaofan.common;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AccessTokenBlacklist {

    private final TokenHashUtil tokenHashUtil;
    private final Map<String, Long> blacklistedTokenHashes = new ConcurrentHashMap<>();

    public AccessTokenBlacklist(TokenHashUtil tokenHashUtil) {
        this.tokenHashUtil = tokenHashUtil;
    }

    public void blacklist(String accessToken, long expiresAtEpochSecond) {
        blacklistedTokenHashes.put(tokenHashUtil.sha256(accessToken), expiresAtEpochSecond);
    }

    public boolean contains(String accessToken) {
        long now = Instant.now().getEpochSecond();
        blacklistedTokenHashes.entrySet().removeIf(entry -> entry.getValue() <= now);
        Long expiresAt = blacklistedTokenHashes.get(tokenHashUtil.sha256(accessToken));
        return expiresAt != null && expiresAt > now;
    }
}
