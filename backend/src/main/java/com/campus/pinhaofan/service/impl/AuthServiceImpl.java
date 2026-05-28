package com.campus.pinhaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.pinhaofan.common.AccessTokenBlacklist;
import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.common.PasswordHashUtil;
import com.campus.pinhaofan.common.TokenHashUtil;
import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.campus.pinhaofan.dto.AuthLogoutRequest;
import com.campus.pinhaofan.dto.AuthRefreshRequest;
import com.campus.pinhaofan.dto.AuthRegisterRequest;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.entity.UserRefreshToken;
import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.UserRefreshTokenMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.AuthService;
import com.campus.pinhaofan.vo.AuthLoginVO;
import com.campus.pinhaofan.vo.AuthLogoutVO;
import com.campus.pinhaofan.vo.AuthRegisterVO;
import com.campus.pinhaofan.vo.AuthUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DISABLED_STATUS = "DISABLED";
    private static final long REFRESH_TOKEN_EXPIRE_DAYS = 14L;

    private final UserMapper userMapper;
    private final UserRefreshTokenMapper userRefreshTokenMapper;
    private final PasswordHashUtil passwordHashUtil;
    private final AuthTokenUtil authTokenUtil;
    private final TokenHashUtil tokenHashUtil;
    private final AccessTokenBlacklist accessTokenBlacklist;

    @Override
    @Transactional
    public AuthRegisterVO register(AuthRegisterRequest request) {
        String username = normalize(request.getUsername());
        String password = request.getPassword();
        String nickname = normalize(request.getNickname());

        if (username.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }
        if (nickname.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "昵称不能为空");
        }
        if (findByAccount(username) != null) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "用户名已存在");
        }

        User user = new User();
        user.setAccount(username);
        user.setPasswordHash(passwordHashUtil.hash(password));
        user.setNickname(nickname);
        user.setStatus(ACTIVE_STATUS);
        user.setPasswordUpdateTime(LocalDateTime.now());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "用户名已存在");
        }

        return new AuthRegisterVO(user.getId(), user.getAccount(), user.getNickname(), user.getStatus());
    }

    @Override
    @Transactional
    public AuthLoginVO login(AuthLoginRequest request, String clientIp) {
        String username = normalize(request.getUsername());
        String password = request.getPassword();

        if (username.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }

        User user = findByAccount(username);
        if (user == null || !passwordHashUtil.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (DISABLED_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginTime(now);
        user.setLastLoginIp(normalize(clientIp).isEmpty() ? null : normalize(clientIp));
        userMapper.updateById(user);

        return issueTokenPair(user);
    }

    @Override
    public AuthUserVO getCurrentUser(String authorization) {
        Long userId = authTokenUtil.parseUserIdFromAuthorization(authorization);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (DISABLED_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已禁用");
        }
        return toUserVO(user);
    }

    @Override
    @Transactional
    public AuthLoginVO refresh(AuthRefreshRequest request) {
        String refreshToken = normalize(request.getRefreshToken());
        if (refreshToken.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "refreshToken 不能为空");
        }

        UserRefreshToken tokenRecord = getValidRefreshToken(refreshToken);
        User user = userMapper.selectById(tokenRecord.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (DISABLED_STATUS.equals(user.getStatus()) || !ACTIVE_STATUS.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "用户已禁用");
        }

        revokeRefreshToken(tokenRecord);
        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public AuthLogoutVO logout(String authorization, AuthLogoutRequest request) {
        String accessToken = authTokenUtil.extractBearerToken(authorization);
        Long userId = authTokenUtil.parseUserIdFromAuthorization(authorization);
        String refreshToken = normalize(request.getRefreshToken());
        if (refreshToken.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "refreshToken 不能为空");
        }

        UserRefreshToken tokenRecord = getValidRefreshToken(refreshToken);
        if (!userId.equals(tokenRecord.getUserId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "refreshToken 已失效");
        }

        revokeRefreshToken(tokenRecord);
        accessTokenBlacklist.blacklist(accessToken, authTokenUtil.parseExpiresAt(accessToken));
        return new AuthLogoutVO(true);
    }

    private AuthLoginVO issueTokenPair(User user) {
        String accessToken = authTokenUtil.createToken(user.getId());
        String refreshToken = createRefreshToken();
        UserRefreshToken tokenRecord = new UserRefreshToken();
        tokenRecord.setUserId(user.getId());
        tokenRecord.setTokenHash(tokenHashUtil.sha256(refreshToken));
        tokenRecord.setExpireTime(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRE_DAYS));
        tokenRecord.setRevoked(false);
        userRefreshTokenMapper.insert(tokenRecord);
        return new AuthLoginVO(accessToken, refreshToken, authTokenUtil.getExpireSeconds(), toUserVO(user));
    }

    private UserRefreshToken getValidRefreshToken(String refreshToken) {
        UserRefreshToken tokenRecord = userRefreshTokenMapper.selectOne(
                new LambdaQueryWrapper<UserRefreshToken>()
                        .eq(UserRefreshToken::getTokenHash, tokenHashUtil.sha256(refreshToken))
                        .last("LIMIT 1")
        );
        if (tokenRecord == null
                || Boolean.TRUE.equals(tokenRecord.getRevoked())
                || tokenRecord.getExpireTime() == null
                || !tokenRecord.getExpireTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "refreshToken 已失效");
        }
        return tokenRecord;
    }

    private void revokeRefreshToken(UserRefreshToken tokenRecord) {
        tokenRecord.setRevoked(true);
        tokenRecord.setRevokedTime(LocalDateTime.now());
        userRefreshTokenMapper.updateById(tokenRecord);
    }

    private String createRefreshToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private User findByAccount(String account) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAccount, account)
                .last("LIMIT 1"));
    }

    private AuthUserVO toUserVO(User user) {
        return new AuthUserVO(
                user.getId(),
                user.getAccount(),
                user.getNickname(),
                user.getPhone(),
                user.getStatus()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
