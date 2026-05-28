package com.campus.pinhaofan.service;

import com.campus.pinhaofan.common.AuthTokenUtil;
import com.campus.pinhaofan.common.PasswordHashUtil;
import com.campus.pinhaofan.common.AccessTokenBlacklist;
import com.campus.pinhaofan.common.TokenHashUtil;
import com.campus.pinhaofan.dto.AuthLoginRequest;
import com.campus.pinhaofan.dto.AuthLogoutRequest;
import com.campus.pinhaofan.dto.AuthRefreshRequest;
import com.campus.pinhaofan.dto.AuthRegisterRequest;
import com.campus.pinhaofan.entity.User;
import com.campus.pinhaofan.entity.UserRefreshToken;
import com.campus.pinhaofan.exception.BusinessException;
import com.campus.pinhaofan.mapper.UserRefreshTokenMapper;
import com.campus.pinhaofan.mapper.UserMapper;
import com.campus.pinhaofan.service.impl.AuthServiceImpl;
import com.campus.pinhaofan.vo.AuthLoginVO;
import com.campus.pinhaofan.vo.AuthLogoutVO;
import com.campus.pinhaofan.vo.AuthRegisterVO;
import com.campus.pinhaofan.vo.AuthUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String RAW_PASSWORD = "P@ssw0rd123";

    @Mock
    private UserMapper userMapper;
    @Mock
    private UserRefreshTokenMapper userRefreshTokenMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private PasswordHashUtil passwordHashUtil;
    private AuthTokenUtil authTokenUtil;
    private TokenHashUtil tokenHashUtil;
    private AccessTokenBlacklist accessTokenBlacklist;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordHashUtil = new PasswordHashUtil();
        tokenHashUtil = new TokenHashUtil();
        accessTokenBlacklist = new AccessTokenBlacklist(tokenHashUtil, stringRedisTemplate);
        authTokenUtil = new AuthTokenUtil("unit-test-secret", 24, accessTokenBlacklist);
        authService = new AuthServiceImpl(
                userMapper,
                userRefreshTokenMapper,
                passwordHashUtil,
                authTokenUtil,
                tokenHashUtil,
                accessTokenBlacklist
        );
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
    void loginReturnsTokenPairAndUserSummaryForActiveUser() {
        User user = activeUser(1001L, "20260001", "小何");
        user.setPasswordHash(passwordHashUtil.hash(RAW_PASSWORD));
        when(userMapper.selectOne(any())).thenReturn(user);
        doAnswer(invocation -> {
            UserRefreshToken token = invocation.getArgument(0);
            token.setId(9001L);
            return 1;
        }).when(userRefreshTokenMapper).insert(any(UserRefreshToken.class));

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("20260001");
        request.setPassword(RAW_PASSWORD);

        AuthLoginVO result = authService.login(request, "127.0.0.1");

        assertThat(authTokenUtil.parseUserId(result.getAccessToken())).isEqualTo(1001L);
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(result.getExpiresIn()).isEqualTo(24 * 3600L);
        assertThat(result.getUser().getId()).isEqualTo(1001L);
        assertThat(result.getUser().getUsername()).isEqualTo("20260001");
        assertThat(result.getUser().getNickname()).isEqualTo("小何");
        assertThat(result.getUser().getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getLastLoginTime()).isNotNull();
        assertThat(userCaptor.getValue().getLastLoginIp()).isEqualTo("127.0.0.1");

        ArgumentCaptor<UserRefreshToken> tokenCaptor = ArgumentCaptor.forClass(UserRefreshToken.class);
        verify(userRefreshTokenMapper).insert(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(tokenHashUtil.sha256(result.getRefreshToken()));
        assertThat(tokenCaptor.getValue().getTokenHash()).isNotEqualTo(result.getRefreshToken());
        assertThat(tokenCaptor.getValue().getRevoked()).isFalse();
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = activeUser(1001L, "20260001", "小何");
        user.setPasswordHash(passwordHashUtil.hash(RAW_PASSWORD));
        when(userMapper.selectOne(any())).thenReturn(user);

        AuthLoginRequest request = new AuthLoginRequest();
        request.setUsername("20260001");
        request.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(401);
                    assertThat(exception.getMessage()).isEqualTo("用户名或密码错误");
                });
        verify(userRefreshTokenMapper, never()).insert(any(UserRefreshToken.class));
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

    @Test
    void refreshRotatesRefreshTokenAndReturnsNewTokenPair() {
        User user = activeUser(1001L, "20260001", "小何");
        String refreshToken = "refresh-token-demo";
        UserRefreshToken existingToken = refreshToken(9001L, 1001L, refreshToken, false);

        when(userRefreshTokenMapper.selectOne(any())).thenReturn(existingToken);
        when(userMapper.selectById(1001L)).thenReturn(user);
        doAnswer(invocation -> {
            UserRefreshToken token = invocation.getArgument(0);
            token.setId(9002L);
            return 1;
        }).when(userRefreshTokenMapper).insert(any(UserRefreshToken.class));

        AuthRefreshRequest request = new AuthRefreshRequest();
        request.setRefreshToken(refreshToken);
        AuthLoginVO result = authService.refresh(request);

        assertThat(authTokenUtil.parseUserId(result.getAccessToken())).isEqualTo(1001L);
        assertThat(result.getRefreshToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotEqualTo(refreshToken);
        assertThat(existingToken.getRevoked()).isTrue();
        assertThat(existingToken.getRevokedTime()).isNotNull();
        verify(userRefreshTokenMapper).updateById(existingToken);
        verify(userRefreshTokenMapper).insert(any(UserRefreshToken.class));
    }

    @Test
    void logoutRevokesRefreshTokenAndBlacklistsAccessToken() {
        String accessToken = authTokenUtil.createToken(1001L);
        String refreshToken = "refresh-token-demo";
        UserRefreshToken existingToken = refreshToken(9001L, 1001L, refreshToken, false);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRefreshTokenMapper.selectOne(any())).thenReturn(existingToken);

        AuthLogoutRequest request = new AuthLogoutRequest();
        request.setRefreshToken(refreshToken);
        AuthLogoutVO result = authService.logout("Bearer " + accessToken, request);

        assertThat(result.getLogout()).isTrue();
        assertThat(existingToken.getRevoked()).isTrue();

        String expectedKey = "auth:blacklist:access:" + tokenHashUtil.sha256(accessToken);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(expectedKey), anyString(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().getSeconds()).isPositive();
        assertThat(ttlCaptor.getValue().getSeconds()).isLessThanOrEqualTo(24 * 3600L);

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);
        assertThatThrownBy(() -> authService.getCurrentUser("Bearer " + accessToken))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(401);
                    assertThat(exception.getMessage()).isEqualTo("登录已失效");
                });
    }

    @Test
    void getCurrentUserFailsWhenRedisBlacklistUnavailable() {
        String accessToken = authTokenUtil.createToken(1001L);
        when(stringRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> authService.getCurrentUser("Bearer " + accessToken))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception.getMessage()).isEqualTo("Redis 黑名单不可用");
                });
    }

    @Test
    void refreshRejectsRevokedToken() {
        String refreshToken = "refresh-token-demo";
        when(userRefreshTokenMapper.selectOne(any())).thenReturn(refreshToken(9001L, 1001L, refreshToken, true));

        AuthRefreshRequest request = new AuthRefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(401);
                    assertThat(exception.getMessage()).isEqualTo("refreshToken 已失效");
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

    private UserRefreshToken refreshToken(Long id, Long userId, String rawToken, boolean revoked) {
        UserRefreshToken token = new UserRefreshToken();
        token.setId(id);
        token.setUserId(userId);
        token.setTokenHash(tokenHashUtil.sha256(rawToken));
        token.setExpireTime(LocalDateTime.now().plusDays(1));
        token.setRevoked(revoked);
        return token;
    }
}
