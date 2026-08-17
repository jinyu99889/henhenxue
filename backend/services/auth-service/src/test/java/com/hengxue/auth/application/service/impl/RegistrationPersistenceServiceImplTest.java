package com.hengxue.auth.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hengxue.auth.config.EmailCodeProperties;
import com.hengxue.auth.config.SnowflakeProperties;
import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.auth.application.service.RegistrationIdempotencyService;
import com.hengxue.auth.application.support.SnowflakeIdGenerator;
import com.hengxue.auth.application.support.VerificationCodeSigner;
import com.hengxue.auth.domain.entity.NewUser;
import com.hengxue.auth.domain.repository.UserRegistrationRepository;
import com.hengxue.auth.domain.repository.VerificationCodeRepository;
import com.hengxue.auth.interfaces.rest.response.UserResponse;
import com.hengxue.common.core.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** 注册数据库事务用例测试。 */
class RegistrationPersistenceServiceImplTest {

    /** 验证注册会创建用户、密码身份和普通用户角色关联。 */
    @Test
    void shouldCreateUserWithUserRoleAfterConsumingCode() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationIdempotencyService idempotencyService = mock(RegistrationIdempotencyService.class);
        when(repository.findActiveUserRoleId()).thenReturn("01J00000000000000000000002");
        when(store.consumeRegistrationCode(anyString(), anyString())).thenReturn(true);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        RegistrationPersistenceServiceImpl service = service(repository, store, encoder, idempotencyService);

        UserResponse user = service.create(command(), "550e8400-e29b-41d4-a716-446655440000");

        ArgumentCaptor<NewUser> userCaptor = ArgumentCaptor.forClass(NewUser.class);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).create(userCaptor.capture(), passwordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("01J00000000000000000000002"), any(LocalDateTime.class));
        assertThat(user.username()).isEqualTo("member_01");
        assertThat(user.email()).isEqualTo("member@example.com");
        assertThat(userCaptor.getValue().id()).matches("^\\d{26}$");
        assertThat(encoder.matches("secret123", passwordCaptor.getValue())).isTrue();
        verify(idempotencyService).complete(anyString(), any(UserResponse.class));
    }

    /** 验证无效验证码不会写入用户或角色关联。 */
    @Test
    void shouldRejectInvalidCodeBeforeCreatingUser() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        when(store.consumeRegistrationCode(anyString(), anyString())).thenReturn(false);
        RegistrationPersistenceServiceImpl service = service(
                repository, store, new BCryptPasswordEncoder(4), mock(RegistrationIdempotencyService.class));

        assertThatThrownBy(() -> service.create(command(), "550e8400-e29b-41d4-a716-446655440000"))
                .isInstanceOf(BusinessException.class);

        verify(repository, never()).create(any(), anyString(), anyString(), any());
    }

    /** 创建测试命令。 */
    private RegisterCommand command() {
        return new RegisterCommand("member_01", "member@example.com", "123456", "secret123", "学习者");
    }

    /** 创建测试用验证码签名器。 */
    private VerificationCodeSigner signer() {
        VerificationCodeSigner signer = new VerificationCodeSigner();
        ReflectionTestUtils.setField(signer, "properties", properties());
        return signer;
    }

    private RegistrationPersistenceServiceImpl service(
            UserRegistrationRepository repository,
            VerificationCodeRepository store,
            BCryptPasswordEncoder encoder,
            RegistrationIdempotencyService idempotencyService
    ) {
        RegistrationPersistenceServiceImpl service = new RegistrationPersistenceServiceImpl();
        ReflectionTestUtils.setField(service, "registrationRepository", repository);
        ReflectionTestUtils.setField(service, "verificationCodeStore", store);
        ReflectionTestUtils.setField(service, "verificationCodeSigner", signer());
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "snowflakeIdGenerator", snowflakeIdGenerator());
        ReflectionTestUtils.setField(service, "idempotencyService", idempotencyService);
        return service;
    }

    private EmailCodeProperties properties() {
        return new EmailCodeProperties("test-secret-value-at-least-32-characters", 600, 60, 600, 5, 5);
    }

    /** 创建测试用雪花 ID 生成器。 */
    private SnowflakeIdGenerator snowflakeIdGenerator() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();
        ReflectionTestUtils.setField(generator, "clock", Clock.systemUTC());
        ReflectionTestUtils.setField(generator, "properties", new SnowflakeProperties(0, 0, 5));
        return generator;
    }
}
