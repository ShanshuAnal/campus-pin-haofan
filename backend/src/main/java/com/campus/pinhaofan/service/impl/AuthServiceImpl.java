package com.campus.pinhaofan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.common.PasswordHashUtil;
import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.campus.pinhaofan.dto.AuthRegisterRequest;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.enums.ResultCode;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.AuthService;
import com.campus.pinhaofan.vo.AuthLoginVO;
import com.campus.pinhaofan.vo.AuthRegisterVO;
import com.campus.pinhaofan.vo.AuthUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DISABLED_STATUS = "DISABLED";

    private final UserMapper userMapper;
    private final PasswordHashUtil passwordHashUtil;
    private final AuthTokenUtil authTokenUtil;

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

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT.getCode(), "用户名已存在");
        }

        return new AuthRegisterVO(user.getId(), user.getAccount(), user.getNickname(), user.getStatus());
    }

    @Override
    public AuthLoginVO login(AuthLoginRequest request) {
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

        return new AuthLoginVO(authTokenUtil.createToken(user.getId()), toUserVO(user));
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
