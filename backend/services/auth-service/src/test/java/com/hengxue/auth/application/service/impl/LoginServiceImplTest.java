package com.hengxue.auth.application.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hengxue.auth.domain.repository.LoginRateLimitRepository;
import com.hengxue.auth.domain.repository.UserLoginRepository;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** 登录应用服务的安全边界测试。 */
@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private UserLoginRepository loginRepository;

    @Mock
    private LoginRateLimitRepository rateLimitRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    void unknownAccountStillRunsDummyPasswordCheckAndReturnsSame401() {
        when(rateLimitRepository.tryAcquire("127.0.0.1", "nobody@example.com")).thenReturn(true);
        when(loginRepository.findActiveByAccount("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(" Nobody@Example.com ", "Password1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ApiErrorCode.UNAUTHENTICATED);

        verify(passwordEncoder).matches("Password1",
                "$2a$12$vlZtwJeEh9eObGsDBB86He9opf5kq2Z5YscYgzApcgBiFmxNm9lzi");
    }

    @Test
    void rateLimitedLoginDoesNotQueryDatabase() {
        when(rateLimitRepository.tryAcquire("127.0.0.1", "user@example.com")).thenReturn(false);

        assertThatThrownBy(() -> loginService.login("user@example.com", "Password1", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ApiErrorCode.RATE_LIMITED);
    }
}
