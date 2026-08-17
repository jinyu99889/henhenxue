package com.hengxue.auth.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.hengxue.auth.config.EmailCodeProperties;
import com.hengxue.auth.application.support.VerificationCodeSigner;
import com.hengxue.auth.domain.enums.AuthErrorCode;
import com.hengxue.auth.domain.repository.UserRegistrationRepository;
import com.hengxue.auth.domain.repository.VerificationCodeRepository;
import com.hengxue.auth.domain.service.RegistrationEmailSender;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

/** 邮箱验证码发送用例测试。 */
class EmailCodeServiceImplTest {

    /** 验证首次发送会保存签名验证码并投递邮件任务。 */
    @Test
    void shouldSaveCodeBeforeSendingRegistrationMail() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        when(store.registrationCodeRemainingSeconds("member@example.com")).thenReturn(0L);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(true);
        when(store.acquireRegistrationSendLock("member@example.com")).thenReturn(true);
        EmailCodeServiceImpl service = service(repository, store, sender, Runnable::run);

        service.sendRegistrationCode("member@example.com", "127.0.0.1");

        verify(store).saveRegistrationCode(anyString(), anyString());
        verify(sender).sendRegistrationCode(anyString(), org.mockito.ArgumentMatchers.matches("^\\d{6}$"));
        verify(store).releaseRegistrationSendLock("member@example.com");
    }

    /** 验证有效验证码存在时会在数据库查询前拒绝重复发送。 */
    @Test
    void shouldRejectRepeatedSendWhileCodeIsActive() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(true);
        when(store.registrationCodeRemainingSeconds("member@example.com")).thenReturn(120L);
        EmailCodeServiceImpl service = service(repository, store, sender, Runnable::run);

        assertThatThrownBy(() -> service.sendRegistrationCode("member@example.com", "127.0.0.1"))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).existsActiveEmail(anyString());
        verify(store, never()).acquireRegistrationSendLock(anyString());
    }

    /** 验证 IP 配额耗尽时不访问 Redis 验证码状态或用户数据库。 */
    @Test
    void shouldRejectIpRateLimitBeforeDatabaseLookup() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(false);
        EmailCodeServiceImpl service = service(repository, store, sender, Runnable::run);

        assertThatThrownBy(() -> service.sendRegistrationCode("member@example.com", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).errorCode())
                        .isEqualTo(ApiErrorCode.RATE_LIMITED));
        verify(repository, never()).existsActiveEmail(anyString());
        verify(store, never()).registrationCodeRemainingSeconds(anyString());
        verify(store, never()).acquireRegistrationSendLock(anyString());
    }

    /** 验证已注册邮箱会立即返回明确业务错误且释放邮箱锁。 */
    @Test
    void shouldReleaseSendLockWhenEmailIsAlreadyRegistered() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(true);
        when(store.registrationCodeRemainingSeconds("member@example.com")).thenReturn(0L);
        when(store.acquireRegistrationSendLock("member@example.com")).thenReturn(true);
        when(repository.existsActiveEmail("member@example.com")).thenReturn(true);
        EmailCodeServiceImpl service = service(repository, store, sender, Runnable::run);

        assertThatThrownBy(() -> service.sendRegistrationCode("member@example.com", "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_REGISTERED));

        verify(store).releaseRegistrationSendLock("member@example.com");
        verify(store, never()).saveRegistrationCode(anyString(), anyString());
        verify(sender, never()).sendRegistrationCode(anyString(), anyString());
    }

    /** 验证 HTTP 线程提交任务后不等待 SMTP，并由后台任务释放邮箱锁。 */
    @Test
    void shouldDeliverRegistrationMailAsynchronously() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        when(store.registrationCodeRemainingSeconds("member@example.com")).thenReturn(0L);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(true);
        when(store.acquireRegistrationSendLock("member@example.com")).thenReturn(true);
        EmailCodeServiceImpl service = service(repository, store, sender, executor);

        service.sendRegistrationCode("member@example.com", "127.0.0.1");

        verify(sender, never()).sendRegistrationCode(anyString(), anyString());
        verify(store, never()).releaseRegistrationSendLock("member@example.com");
        executor.runTask();
        verify(sender).sendRegistrationCode(anyString(), org.mockito.ArgumentMatchers.matches("^\\d{6}$"));
        verify(store).releaseRegistrationSendLock("member@example.com");
    }

    /** 验证异步 SMTP 投递失败时会撤销验证码并释放邮箱锁。 */
    @Test
    void shouldRemoveCodeAndReleaseLockWhenAsynchronousDeliveryFails() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        QueuedTaskExecutor executor = new QueuedTaskExecutor();
        when(store.registrationCodeRemainingSeconds("member@example.com")).thenReturn(0L);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(true);
        when(store.acquireRegistrationSendLock("member@example.com")).thenReturn(true);
        org.mockito.Mockito.doThrow(new BusinessException(ApiErrorCode.SERVICE_UNAVAILABLE))
                .when(sender).sendRegistrationCode(anyString(), anyString());
        EmailCodeServiceImpl service = service(repository, store, sender, executor);

        service.sendRegistrationCode("member@example.com", "127.0.0.1");
        executor.runTask();

        verify(store).removeRegistrationCodeIfMatches(anyString(), anyString());
        verify(store).releaseRegistrationSendLock("member@example.com");
    }

    /** 验证邮件任务队列饱和时撤销验证码、释放邮箱锁并返回服务不可用。 */
    @Test
    void shouldRollbackCodeWhenEmailTaskIsRejected() {
        UserRegistrationRepository repository = mock(UserRegistrationRepository.class);
        VerificationCodeRepository store = mock(VerificationCodeRepository.class);
        RegistrationEmailSender sender = mock(RegistrationEmailSender.class);
        when(store.registrationCodeRemainingSeconds("member@example.com")).thenReturn(0L);
        when(store.tryAcquireSendQuota("127.0.0.1")).thenReturn(true);
        when(store.acquireRegistrationSendLock("member@example.com")).thenReturn(true);
        EmailCodeServiceImpl service = service(repository, store, sender,
                task -> {
                    throw new TaskRejectedException("邮件任务队列已满");
                });

        assertThatThrownBy(() -> service.sendRegistrationCode("member@example.com", "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.SERVICE_UNAVAILABLE));
        verify(store).removeRegistrationCodeIfMatches(anyString(), anyString());
        verify(store).releaseRegistrationSendLock("member@example.com");
        verify(sender, never()).sendRegistrationCode(anyString(), anyString());
    }

    /** 验证发送验证码方法声明了 Sentinel 资源和业务异常豁免。 */
    @Test
    void shouldDeclareSentinelResourceForRegistrationCode() throws NoSuchMethodException {
        SentinelResource annotation = EmailCodeServiceImpl.class
                .getMethod("sendRegistrationCode", String.class, String.class)
                .getAnnotation(SentinelResource.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("auth.email-code.send-registration");
        assertThat(annotation.blockHandler()).isEqualTo("handleRegistrationCodeBlocked");
        assertThat(annotation.fallback()).isEqualTo("handleRegistrationCodeFailure");
        assertThat(annotation.exceptionsToIgnore()).containsExactly(BusinessException.class);
    }

    /** 验证 Sentinel 限流和未知异常分别映射为稳定的业务错误。 */
    @Test
    void shouldMapSentinelAndUnexpectedFailuresToBusinessErrors() {
        EmailCodeServiceImpl service = service(
                mock(UserRegistrationRepository.class),
                mock(VerificationCodeRepository.class),
                mock(RegistrationEmailSender.class),
                Runnable::run
        );

        assertThatThrownBy(() -> service.handleRegistrationCodeBlocked(
                "member@example.com", "127.0.0.1", new FlowException("auth.email-code.send-registration")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.RATE_LIMITED));
        assertThatThrownBy(() -> service.handleRegistrationCodeFailure(
                "member@example.com", "127.0.0.1", new IllegalStateException()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ApiErrorCode.INTERNAL_ERROR));
    }

    /** 创建测试用验证码签名器。 */
    private VerificationCodeSigner signer() {
        VerificationCodeSigner signer = new VerificationCodeSigner();
        ReflectionTestUtils.setField(signer, "properties", properties());
        return signer;
    }

    /**
     * 创建注入依赖后的应用服务测试对象。
     *
     * @param repository 用户注册仓储
     * @param store 验证码仓储
     * @param sender 邮件发送端口
     * @param taskExecutor 邮件任务执行器
     * @return 已注入依赖的应用服务
     */
    private EmailCodeServiceImpl service(
            UserRegistrationRepository repository,
            VerificationCodeRepository store,
            RegistrationEmailSender sender,
            TaskExecutor taskExecutor
    ) {
        EmailCodeServiceImpl service = new EmailCodeServiceImpl();
        ReflectionTestUtils.setField(service, "registrationRepository", repository);
        ReflectionTestUtils.setField(service, "verificationCodeStore", store);
        ReflectionTestUtils.setField(service, "registrationEmailSender", sender);
        ReflectionTestUtils.setField(service, "verificationCodeSigner", signer());
        ReflectionTestUtils.setField(service, "registrationEmailTaskExecutor", taskExecutor);
        return service;
    }

    /** 用于精确控制异步任务执行时机的测试执行器。 */
    private static final class QueuedTaskExecutor implements TaskExecutor {

        private Runnable task;

        /**
         * 保存待执行任务。
         *
         * @param task 待执行任务
         */
        @Override
        public void execute(Runnable task) {
            this.task = task;
        }

        /** 执行已保存的任务。 */
        private void runTask() {
            assertThat(task).isNotNull();
            task.run();
        }
    }

    private EmailCodeProperties properties() {
        return new EmailCodeProperties("test-secret-value-at-least-32-characters", 600, 60, 600, 5, 5);
    }
}
