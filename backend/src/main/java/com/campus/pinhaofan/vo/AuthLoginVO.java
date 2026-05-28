package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginVO {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private AuthUserVO user;
}
