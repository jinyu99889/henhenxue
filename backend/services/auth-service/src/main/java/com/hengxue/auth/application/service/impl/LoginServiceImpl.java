package com.hengxue.auth.application.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hengxue.auth.application.service.LoginService;
import com.hengxue.auth.domain.entity.LoginUser;
import com.hengxue.auth.domain.repository.LoginRateLimitRepository;
import com.hengxue.auth.domain.repository.UserLoginRepository;
import com.hengxue.auth.interfaces.rest.response.LoginResponse;
import com.hengxue.auth.interfaces.rest.response.UserResponse;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** 负责账号密码校验、会话创建和登录审计的应用服务。 */
@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServiceImpl.class);
    private static final String LOGIN_RESOURCE = "auth.login.submit";
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$vlZtwJeEh9eObGsDBB86He9opf5kq2Z5YscYgzApcgBiFmxNm9lzi";

    @Autowired
    private UserLoginRepository loginRepository;

    @Autowired
    private LoginRateLimitRepository rateLimitRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    /**
     * 执行登录并创建独立设备会话。
     *
     * @param account 用户名或邮箱
     * @param password 明文密码
     * @param sourceIp 请求来源 IP
     * @return Sa-Token 令牌和用户摘要
     */
    @Override
    @SentinelResource(value = LOGIN_RESOURCE, blockHandler = "handleLoginBlocked")
    public LoginResponse login(String account, String password, String sourceIp) {
        String normalizedAccount = normalizeAccount(account);
        LOGGER.info("登录请求进入，账号摘要={}，来源IP={}", mask(normalizedAccount), sourceIp);
        try {
            if (!rateLimitRepository.tryAcquire(sourceIp, normalizedAccount)) {
                LOGGER.warn("登录请求触发限流，账号摘要={}，来源IP={}", mask(normalizedAccount), sourceIp);
                throw new BusinessException(ApiErrorCode.RATE_LIMITED, "登录尝试过于频繁，请稍后重试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            LOGGER.error("登录限流依赖不可用，账号摘要={}", mask(normalizedAccount), exception);
            throw new BusinessException(ApiErrorCode.SERVICE_UNAVAILABLE, "登录服务暂不可用，请稍后重试", exception);
        }

        Optional<LoginUser> user;
        try {
            user = loginRepository.findActiveByAccount(normalizedAccount);
        } catch (DataAccessException exception) {
            LOGGER.error("登录用户查询失败，账号摘要={}", mask(normalizedAccount), exception);
            throw new BusinessException(ApiErrorCode.SERVICE_UNAVAILABLE, "登录服务暂不可用，请稍后重试", exception);
        }
        boolean passwordMatches = user.map(value -> passwordEncoder.matches(password, value.passwordHash()))
                .orElseGet(() -> passwordEncoder.matches(password, DUMMY_PASSWORD_HASH));
        if (!passwordMatches || user.isEmpty()) {
            LOGGER.warn("登录凭证校验失败，账号摘要={}，来源IP={}", mask(normalizedAccount), sourceIp);
            throw new BusinessException(ApiErrorCode.UNAUTHENTICATED, "账号或密码错误");
        }

        LoginUser loginUser = user.get();
        List<String> permissions;
        LocalDateTime loginAt = LocalDateTime.now(clock);
        try {
            permissions = loginRepository.findActivePermissions(loginUser.id());
            loginRepository.updateLastLoginAt(loginUser.id(), loginAt);
        } catch (DataAccessException exception) {
            LOGGER.error("登录成功后的用户状态写入失败，用户ID={}", loginUser.id(), exception);
            throw new BusinessException(ApiErrorCode.SERVICE_UNAVAILABLE, "登录服务暂不可用，请稍后重试", exception);
        }
        StpUtil.login(loginUser.id());
        String token = StpUtil.getTokenValue();
        UserResponse response = new UserResponse(
                loginUser.id(),
                loginUser.username(),
                loginUser.email(),
                loginUser.emailVerifiedAt(),
                loginUser.nickname(),
                null,
                permissions,
                loginUser.version());
        LOGGER.info("登录成功，用户ID={}，账号摘要={}，来源IP={}", loginUser.id(), mask(normalizedAccount), sourceIp);
        return new LoginResponse(token, response);
    }

    /**
     * 注销当前设备的会话。
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * Sentinel 限流处理。
     *
     * @param account 用户名或邮箱
     * @param password 明文密码
     * @param sourceIp 请求来源 IP
     * @param exception Sentinel 流控异常
     * @return 此方法始终抛出业务异常
     */
    public LoginResponse handleLoginBlocked(
            String account,
            String password,
            String sourceIp,
            BlockException exception
    ) {
        throw new BusinessException(ApiErrorCode.RATE_LIMITED, "登录尝试过于频繁，请稍后重试", exception);
    }

    /**
     * 规范化登录账号；邮箱按不区分大小写处理。
     *
     * @param account 原始账号
     * @return 规范化账号
     */
    private String normalizeAccount(String account) {
        String value = account.strip();
        return value.contains("@") ? value.toLowerCase(Locale.ROOT) : value;
    }

    /**
     * 仅保留少量可用于排障的账号信息，避免日志泄露完整邮箱或用户名。
     *
     * @param value 账号
     * @return 脱敏账号
     */
    private String mask(String value) {
        if (value.length() <= 3) {
            return "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 1);
    }
}
