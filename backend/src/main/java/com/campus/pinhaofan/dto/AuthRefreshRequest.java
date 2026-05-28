package com.campus.pinhaofan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRefreshRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
