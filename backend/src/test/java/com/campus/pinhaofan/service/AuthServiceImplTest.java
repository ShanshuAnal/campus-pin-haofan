package com.campus.pinhaofan.service;

import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.common.PasswordHashUtil;
import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.campus.pinhaofan.dto.AuthRegisterRequest;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.impl.AuthServiceImpl;
import com.campus.pinhaofan.vo.AuthLoginVO;
import com.campus.pinhaofan.vo.AuthRegisterVO;
import com.campus.pinhaofan.vo.AuthUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "P@ssw0rd123";

    @Mock
    private UserMapper userMapper;

    private PasswordHashUtil passwordHashUtil;
    private AuthTokenUtil authTokenUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordHashUtil = new PasswordHashUtil();
        authTokenUtil = new AuthTokenUtil("unit-test-secret", 24);
        authService = new AuthServiceImpl(userMapper, passwordHashUtil, authTokenUtil);
    }

    @Test
    void registerCreatesActiveUserWithHashedPassword() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername(" 20260001 ");
        request.setPassword(RAW_PASSWORD);
        request.setNickname(" 小何 ");

        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1001L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        AuthRegisterVO result = authService.register(request);

        assertThat(result.getUserId()).isEqualTo(1001L);
        assertThat(result.getUsername()).isEqualTo("20260001");
        assertThat(result.getNickname()).isEqualTo("小何");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User insertedUser = userCaptor.getValue();
        assertThat(insertedUser.getAccount()).isEqualTo("20260001");
        assertThat(insertedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordHashUtil.matches(RAW_PASSWORD, insertedUser.getPasswordHash())).isTrue();
        assertThat(insertedUser.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setUsername("20260001");
        request.setPassword(RAW_PASSWORD);
        request.setNickname("小何");

        when(userMapper.selectOne(any())).thenReturn(activeUser(1001L, "20260001", "小何"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(409);
                    assertThat(exception.getMessage()).isEqualTo("用户名已存在");
                });
    }

    @Test
    void loginReturnsTokenAndUserSummaryForActiveUser() {
        User user = activeUser(1001L, "20260001", "小何");
        user.setPasswordHash(passwordHashUtil.hash(RAW_PASSWORD));
        when(userMapper.selectOne(any())).thenReturn(user);

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("20260001");
        request.setPassword(RAW_PASSWORD);

        AuthLoginVO result = authService.login(request);

        assertThat(authTokenUtil.parseUserId(result.getToken())).isEqualTo(1001L);
        assertThat(result.getUser().getId()).isEqualTo(1001L);
        assertThat(result.getUser().getUsername()).isEqualTo("20260001");
        assertThat(result.getUser().getNickname()).isEqualTo("小何");
        assertThat(result.getUser().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = activeUser(1001L, "20260001", "小何");
        user.setPasswordHash(passwordHashUtil.hash(RAW_PASSWORD));
        when(userMapper.selectOne(any())).thenReturn(user);

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("20260001");
        request.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(401);
                    assertThat(exception.getMessage()).isEqualTo("用户名或密码错误");
                });
    }

    @Test
    void getCurrentUserLoadsUserFromBearerToken() {
        User user = activeUser(1001L, "20260001", "小何");
        when(userMapper.selectById(1001L)).thenReturn(user);

        AuthUserVO result = authService.getCurrentUser("Bearer " + authTokenUtil.createToken(1001L));

        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getUsername()).isEqualTo("20260001");
        assertThat(result.getNickname()).isEqualTo("小何");
    }

    @Test
    void getCurrentUserRejectsMissingAuthorization() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(401);
                    assertThat(exception.getMessage()).isEqualTo("未登录");
                });
    }

    private User activeUser(Long id, String account, String nickname) {
        User user = new User();
        user.setId(id);
        user.setAccount(account);
        user.setNickname(nickname);
        user.setStatus("ACTIVE");
        return user;
    }
}
