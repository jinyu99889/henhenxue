package com.hengxue.auth.infrastructure.persistence.redis;

import com.hengxue.auth.config.EmailCodeProperties;
import com.hengxue.auth.domain.repository.VerificationCodeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/** 基于 Redis 的验证码、验证码尝试次数与发送频率存储。 */
@Repository
public class RedisVerificationCodeRepository implements VerificationCodeRepository {

    private static final String CODE_PREFIX = "auth:email-code:register:";
    private static final String SEND_LOCK_PREFIX = "auth:email-code:send-lock:";
    private static final String SEND_RATE_PREFIX = "auth:email-code:send-rate:";
    private static final String VERIFY_ATTEMPT_PREFIX = "auth:email-code:verify-attempt:";
    private static final DefaultRedisScript<Long> COMPARE_DELETE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end return 0", Long.class);
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('DEL', KEYS[1]); redis.call('DEL', KEYS[2]); return 1 end "
                    + "local attempts = redis.call('INCR', KEYS[2]); if attempts == 1 then redis.call('EXPIRE', KEYS[2], ARGV[2]) end "
                    + "if attempts >= tonumber(ARGV[3]) then redis.call('DEL', KEYS[1]); redis.call('DEL', KEYS[2]); end return 0", Long.class);
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "if count > tonumber(ARGV[2]) then return 0 end return 1", Long.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private EmailCodeProperties properties;

    /**
     * 获取验证码剩余有效期。
     *
     * @param email 规范化后的邮箱
     * @return 剩余秒数；键不存在或无过期时间时为零
     */
    @Override
    public long registrationCodeRemainingSeconds(String email) {
        Long seconds = redisTemplate.getExpire(codeKey(email));
        return seconds == null || seconds <= 0 ? 0 : seconds;
    }

    /**
     * 原子获取发送锁。
     *
     * @param email 规范化后的邮箱
     * @return 成功获取时为 {@code true}
     */
    @Override
    public boolean acquireRegistrationSendLock(String email) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                sendLockKey(email), "1", Duration.ofSeconds(properties.sendLockSeconds())));
    }

    /**
     * 释放发送锁。
     *
     * @param email 规范化后的邮箱
     */
    @Override
    public void releaseRegistrationSendLock(String email) {
        redisTemplate.delete(sendLockKey(email));
    }

    /**
     * 原子增加来源 IP 发送计数。
     *
     * @param sourceIp 客户端来源 IP
     * @return 未超限时为 {@code true}
     */
    @Override
    public boolean tryAcquireSendQuota(String sourceIp) {
        Long allowed = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(SEND_RATE_PREFIX + digest(sourceIp)),
                String.valueOf(properties.ipWindowSeconds()),
                String.valueOf(properties.ipMaxSends())
        );
        return Long.valueOf(1L).equals(allowed);
    }

    /**
     * 保存验证码签名和有效期。
     *
     * @param email 规范化后的邮箱
     * @param signedCode 验证码签名
     */
    @Override
    public void saveRegistrationCode(String email, String signedCode) {
        redisTemplate.opsForValue().set(codeKey(email), signedCode, Duration.ofSeconds(properties.validitySeconds()));
    }

    /**
     * 删除当前请求拥有的验证码。
     *
     * @param email 规范化后的邮箱
     * @param signedCode 当前验证码签名
     */
    @Override
    public void removeRegistrationCodeIfMatches(String email, String signedCode) {
        redisTemplate.execute(COMPARE_DELETE_SCRIPT, List.of(codeKey(email)), signedCode);
    }

    /**
     * 原子比对并单次消费验证码，同时记录失败次数。
     *
     * @param email 规范化后的邮箱
     * @param signedCode 提交验证码的签名
     * @return 验证通过时为 {@code true}
     */
    @Override
    public boolean consumeRegistrationCode(String email, String signedCode) {
        Long result = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(codeKey(email), verifyAttemptKey(email)),
                signedCode,
                String.valueOf(properties.validitySeconds()),
                String.valueOf(properties.maxVerifyAttempts())
        );
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 构造验证码键。
     *
     * @param email 规范化后的邮箱
     * @return 不包含明文邮箱的 Redis 键
     */
    private String codeKey(String email) {
        return CODE_PREFIX + digest(email);
    }

    /**
     * 构造发送锁键。
     *
     * @param email 规范化后的邮箱
     * @return 不包含明文邮箱的 Redis 键
     */
    private String sendLockKey(String email) {
        return SEND_LOCK_PREFIX + digest(email);
    }

    /**
     * 构造验证码失败次数键。
     *
     * @param email 规范化后的邮箱
     * @return 不包含明文邮箱的 Redis 键
     */
    private String verifyAttemptKey(String email) {
        return VERIFY_ATTEMPT_PREFIX + digest(email);
    }

    /**
     * 计算不可逆的键片段。
     *
     * @param value 待摘要的值
     * @return 小写 SHA-256 十六进制摘要
     */
    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(Character.forDigit((item >>> 4) & 0x0F, 16));
                builder.append(Character.forDigit(item & 0x0F, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
