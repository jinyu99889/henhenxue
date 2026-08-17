package com.hengxue.auth.application.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hengxue.auth.application.service.EmailCodeService;
import com.hengxue.auth.application.support.VerificationCodeSigner;
import com.hengxue.auth.domain.enums.AuthErrorCode;
import com.hengxue.auth.domain.repository.UserRegistrationRepository;
import com.hengxue.auth.domain.repository.VerificationCodeRepository;
import com.hengxue.auth.domain.service.RegistrationEmailSender;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import com.hengxue.common.observability.TraceIdContext;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 注册邮箱验证码的发送用例。 */
@Service
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final String SEND_REGISTRATION_CODE_RESOURCE = "auth.email-code.send-registration";
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Autowired
    private UserRegistrationRepository registrationRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeStore;

    @Autowired
    private RegistrationEmailSender registrationEmailSender;

    @Autowired
    private VerificationCodeSigner verificationCodeSigner;

    @Autowired
    @Qualifier("registrationEmailTaskExecutor")
    private TaskExecutor registrationEmailTaskExecutor;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 发送注册验证码。
     *
     * @param rawEmail 客户端提交的邮箱
     * @param sourceIp 经网关识别的来源 IP
     */
    @Override
    @SentinelResource(
            value = SEND_REGISTRATION_CODE_RESOURCE,
            blockHandler = "handleRegistrationCodeBlocked",
            fallback = "handleRegistrationCodeFailure",
            exceptionsToIgnore = BusinessException.class
    )
    public void sendRegistrationCode(String rawEmail, String sourceIp) {
        String email = normalizeEmail(rawEmail);
        String maskedEmail = maskEmail(email);
        LOGGER.info("收到注册验证码发送请求，邮箱={}", maskedEmail);

        if (!verificationCodeStore.tryAcquireSendQuota(sourceIp)) {
            LOGGER.warn("注册验证码请求被 IP 限流拒绝，邮箱={}", maskedEmail);
            throw new BusinessException(ApiErrorCode.RATE_LIMITED);
        }
        if (verificationCodeStore.registrationCodeRemainingSeconds(email) > 0) {
            LOGGER.info("注册验证码请求被有效验证码拦截，邮箱={}", maskedEmail);
            throw new BusinessException(ApiErrorCode.RATE_LIMITED);
        }
        if (!verificationCodeStore.acquireRegistrationSendLock(email)) {
            LOGGER.info("注册验证码请求被邮箱发送锁拦截，邮箱={}", maskedEmail);
            throw new BusinessException(ApiErrorCode.RATE_LIMITED);
        }

        boolean releaseLock = true;
        try {
            // 获取邮箱锁后再次检查，防止前一个请求刚生成验证码时发生重复发送。
            if (verificationCodeStore.registrationCodeRemainingSeconds(email) > 0) {
                LOGGER.info("注册验证码请求在获取发送锁后被有效验证码拦截，邮箱={}", maskedEmail);
                throw new BusinessException(ApiErrorCode.RATE_LIMITED);
            }
            if (registrationRepository.existsActiveEmail(email)) {
                LOGGER.info("注册验证码请求被已注册邮箱拒绝，邮箱={}", maskedEmail);
                throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
            }

            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            String signedCode = verificationCodeSigner.sign(email, code);
            verificationCodeStore.saveRegistrationCode(email, signedCode);
            try {
                String traceId = TraceIdContext.getOrCreate();
                registrationEmailTaskExecutor.execute(() -> deliverRegistrationCode(email, code, signedCode, traceId));
                releaseLock = false;
                LOGGER.info("注册验证码已生成并提交邮件投递任务，邮箱={}", maskedEmail);
            } catch (TaskRejectedException exception) {
                verificationCodeStore.removeRegistrationCodeIfMatches(email, signedCode);
                LOGGER.warn("注册验证码邮件任务被拒绝，已撤销验证码，邮箱={}，异常类型={}",
                        maskedEmail, exception.getClass().getName());
                throw new BusinessException(ApiErrorCode.SERVICE_UNAVAILABLE, "验证码发送任务繁忙，请稍后重试", exception);
            }
        } finally {
            if (releaseLock) {
                verificationCodeStore.releaseRegistrationSendLock(email);
            }
        }
    }

    /**
     * 在后台投递注册验证码邮件，并在失败时撤销不可用的验证码。
     *
     * @param email 规范化后的收件邮箱
     * @param code 六位数字验证码
     * @param signedCode 当前验证码的签名
     * @param traceId 触发请求的链路标识
     */
    private void deliverRegistrationCode(String email, String code, String signedCode, String traceId) {
        String previousTraceId = TraceIdContext.currentTraceId();
        TraceIdContext.bind(traceId);
        try {
            registrationEmailSender.sendRegistrationCode(email, code);
            LOGGER.info("注册验证码邮件投递成功，邮箱={}", maskEmail(email));
        } catch (RuntimeException exception) {
            verificationCodeStore.removeRegistrationCodeIfMatches(email, signedCode);
            LOGGER.warn("注册验证码邮件投递失败，已撤销验证码，邮箱={}，异常类型={}",
                    maskEmail(email), exception.getClass().getName());
        } finally {
            verificationCodeStore.releaseRegistrationSendLock(email);
            restoreTraceId(previousTraceId);
        }
    }

    /**
     * 处理 Sentinel 对发送验证码资源实施的流控。
     *
     * @param rawEmail 客户端提交的邮箱
     * @param sourceIp 经网关识别的来源 IP
     * @param exception Sentinel 流控异常
     */
    public void handleRegistrationCodeBlocked(String rawEmail, String sourceIp, BlockException exception) {
        LOGGER.warn("注册验证码请求被 Sentinel 限流拒绝，邮箱={}", maskEmail(rawEmail));
        throw new BusinessException(ApiErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试", exception);
    }

    /**
     * 将发送验证码过程中的未知异常转换为统一的公开错误。
     *
     * @param rawEmail 客户端提交的邮箱
     * @param sourceIp 经网关识别的来源 IP
     * @param exception 未被忽略的运行时异常
     */
    public void handleRegistrationCodeFailure(String rawEmail, String sourceIp, Throwable exception) {
        LOGGER.error("注册验证码请求发生未预期异常，邮箱={}，异常类型={}",
                maskEmail(rawEmail), exception.getClass().getName());
        throw new BusinessException(ApiErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试", exception);
    }

    /**
     * 规范化邮箱用于查询和 Redis 键计算。
     *
     * @param rawEmail 客户端提交的邮箱
     * @return 去除首尾空白并转为小写的邮箱
     */
    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            throw new BusinessException(AuthErrorCode.EMAIL_CODE_INVALID);
        }
        return rawEmail.strip().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 对邮箱做最小展示化处理，保留人工排查所需的识别信息。
     *
     * @param email 原始或规范化后的邮箱
     * @return 掩码后的邮箱；无法识别时返回固定占位符
     */
    private String maskEmail(String email) {
        if (email == null) {
            return "<空>";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "<格式无效>";
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    /**
     * 恢复异步工作线程原有的链路上下文，避免线程复用导致请求串联。
     *
     * @param previousTraceId 工作线程执行任务前的链路标识
     */
    private void restoreTraceId(String previousTraceId) {
        if (previousTraceId == null) {
            TraceIdContext.clear();
            return;
        }
        TraceIdContext.bind(previousTraceId);
    }
}
