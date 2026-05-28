package com.campus.pinhaofan.controller;

import com.campus.pinhaofan.common.Result;
import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.campus.pinhaofan.dto.AuthLogoutRequest;
import com.campus.pinhaofan.dto.AuthRefreshRequest;
import com.campus.pinhaofan.dto.AuthRegisterRequest;
import com.campus.pinhaofan.service.AuthService;
import com.campus.pinhaofan.vo.AuthLoginVO;
import com.campus.pinhaofan.vo.AuthLogoutVO;
import com.campus.pinhaofan.vo.AuthRegisterVO;
import com.campus.pinhaofan.vo.AuthUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<AuthRegisterVO> register(@Valid @RequestBody AuthRegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthLoginVO> login(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletRequest httpServletRequest) {
        return Result.success(authService.login(request, resolveClientIp(httpServletRequest)));
    }

    @GetMapping("/me")
    public Result<AuthUserVO> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(authService.getCurrentUser(authorization));
    }

    @PostMapping("/refresh")
    public Result<AuthLoginVO> refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return Result.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<AuthLogoutVO> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AuthLogoutRequest request) {
        return Result.success(authService.logout(authorization, request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
