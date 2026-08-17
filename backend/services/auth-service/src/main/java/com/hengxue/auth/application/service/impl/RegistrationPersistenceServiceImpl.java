package com.hengxue.auth.application.service.impl;

import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.auth.application.service.RegistrationIdempotencyService;
import com.hengxue.auth.application.service.RegistrationPersistenceService;
import com.hengxue.auth.application.support.SnowflakeIdGenerator;
import com.hengxue.auth.application.support.VerificationCodeSigner;
import com.hengxue.auth.domain.entity.NewUser;
import com.hengxue.auth.domain.enums.AuthErrorCode;
import com.hengxue.auth.domain.repository.UserRegistrationRepository;
import com.hengxue.auth.domain.repository.VerificationCodeRepository;
import com.hengxue.auth.interfaces.rest.response.UserResponse;
import com.hengxue.common.core.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在本地事务内完成用户、密码身份、角色关联与幂等记录写入。 */
@Service
public class RegistrationPersistenceServiceImpl implements RegistrationPersistenceService {

    @Autowired private UserRegistrationRepository registrationRepository;
    @Autowired private VerificationCodeRepository verificationCodeStore;
    @Autowired private VerificationCodeSigner verificationCodeSigner;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired private RegistrationIdempotencyService idempotencyService;

    /**
     * 在单个数据库事务中创建注册用户。
     *
     * @param command 经接口校验后的注册命令
     * @param idempotencyKey 客户端幂等键
     * @return 新注册的用户摘要
     */
    @Transactional
    @Override
    public UserResponse create(RegisterCommand command, String idempotencyKey) {
        String username = normalizeRequired(command.username());
        String email = command.email().strip().toLowerCase(java.util.Locale.ROOT);
        String nickname = normalizeRequired(command.nickname());
        ensureUserDoesNotExist(username, email);

        String signedCode = verificationCodeSigner.sign(email, command.emailCode());
        if (!verificationCodeStore.consumeRegistrationCode(email, signedCode)) {
            throw new BusinessException(AuthErrorCode.EMAIL_CODE_INVALID);
        }

        String roleId = registrationRepository.findActiveUserRoleId();
        if (roleId == null) {
            throw new BusinessException(AuthErrorCode.USER_ROLE_UNAVAILABLE);
        }

        String userId = snowflakeIdGenerator.next();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        NewUser user = new NewUser(userId, username, email, nickname);
        try {
            registrationRepository.create(user, passwordEncoder.encode(command.password()), roleId, now);
        } catch (DuplicateKeyException exception) {
            ensureUserDoesNotExist(username, email);
            throw exception;
        }

        UserResponse response = new UserResponse(
                userId, username, email, now, nickname, null, List.of(), 1);
        idempotencyService.complete(idempotencyKey, response);
        return response;
    }

    /**
     * 检查用户名和邮箱是否已经注册。
     *
     * @param username 规范化后的用户名
     * @param email 规范化后的邮箱
     */
    private void ensureUserDoesNotExist(String username, String email) {
        if (registrationRepository.existsActiveUsername(username)) {
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (registrationRepository.existsActiveEmail(email)) {
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }

    /**
     * 规范化不可为空的显示或登录文本。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本
     */
    private String normalizeRequired(String value) {
        return value.strip();
    }
}
