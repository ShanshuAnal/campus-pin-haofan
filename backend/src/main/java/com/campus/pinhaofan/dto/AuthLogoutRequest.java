package com.campus.pinhaofan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLogoutRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
