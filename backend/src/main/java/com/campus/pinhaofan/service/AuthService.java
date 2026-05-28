package com.campus.pinhaofan.service;

import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.campus.pinhaofan.dto.AuthLogoutRequest;
import com.campus.pinhaofan.dto.AuthRefreshRequest;
import com.campus.pinhaofan.dto.AuthRegisterRequest;
import com.campus.pinhaofan.vo.AuthLoginVO;
import com.campus.pinhaofan.vo.AuthLogoutVO;
import com.campus.pinhaofan.vo.AuthRegisterVO;
import com.campus.pinhaofan.vo.AuthUserVO;

public interface AuthService {

    AuthRegisterVO register(AuthRegisterRequest request);

    AuthLoginVO login(AuthLoginRequest request, String clientIp);

    AuthUserVO getCurrentUser(String authorization);

    AuthLoginVO refresh(AuthRefreshRequest request);

    AuthLogoutVO logout(String authorization, AuthLogoutRequest request);
}
