package com.campus.pinhaofan.common;

import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogAspectTest {

    @Test
    void toSafeJsonMasksSensitiveFieldsAndBearerTokenValues() {
        OperationLogAspect aspect = new OperationLogAspect(new ObjectMapper(), new AuthTokenUtil("unit-test-secret", 24));
        AuthLoginRequest loginRequest = new AuthLoginRequest();
        loginRequest.setUsername("student001");
        loginRequest.setPassword("PlainPassword123");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request", loginRequest);
        payload.put("Authorization", "Bearer access-token-value");
        payload.put("accessToken", "access-token-value");
        payload.put("refreshToken", "refresh-token-value");
        payload.put("secretKey", "secret-key-value");
        payload.put("remark", "visible");

        String json = aspect.toSafeJson(payload);

        assertThat(json).contains("\"password\":\"***\"");
        assertThat(json).contains("\"Authorization\":\"***\"");
        assertThat(json).contains("\"accessToken\":\"***\"");
        assertThat(json).contains("\"refreshToken\":\"***\"");
        assertThat(json).contains("\"secretKey\":\"***\"");
        assertThat(json).contains("visible");
        assertThat(json).doesNotContain("PlainPassword123");
        assertThat(json).doesNotContain("access-token-value");
        assertThat(json).doesNotContain("refresh-token-value");
        assertThat(json).doesNotContain("secret-key-value");
    }
}
