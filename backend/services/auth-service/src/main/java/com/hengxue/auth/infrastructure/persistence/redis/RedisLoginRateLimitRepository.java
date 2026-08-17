package com.hengxue.auth.infrastructure.persistence.redis;

import com.hengxue.auth.config.AuthLoginProperties;
import com.hengxue.auth.domain.repository.LoginRateLimitRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/** 使用 Lua 保证 IP 与账号双维度计数原子性的登录限流仓储。 */
@Repository
public class RedisLoginRateLimitRepository implements LoginRateLimitRepository {

    private static final String IP_PREFIX = "auth:login:ip:";
    private static final String ACCOUNT_PREFIX = "auth:login:account:";
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local ipCount = tonumber(redis.call('GET', KEYS[1]) or '0') "
                    + "local accountCount = tonumber(redis.call('GET', KEYS[2]) or '0') "
                    + "if ipCount >= tonumber(ARGV[2]) or accountCount >= tonumber(ARGV[4]) then return 0 end "
                    + "local nextIp = redis.call('INCR', KEYS[1]) "
                    + "if nextIp == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "local nextAccount = redis.call('INCR', KEYS[2]) "
                    + "if nextAccount == 1 then redis.call('EXPIRE', KEYS[2], ARGV[3]) end "
                    + "return 1",
            Long.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AuthLoginProperties properties;

    /**
     * 原子消耗两个维度的登录尝试额度。
     *
     * @param sourceIp 请求来源 IP
     * @param account 规范化后的用户名或邮箱
     * @return 未超限时为 {@code true}
     */
    @Override
    public boolean tryAcquire(String sourceIp, String account) {
        Long allowed = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(IP_PREFIX + digest(sourceIp), ACCOUNT_PREFIX + digest(account)),
                String.valueOf(properties.ipWindowSeconds()),
                String.valueOf(properties.ipMaxAttempts()),
                String.valueOf(properties.accountWindowSeconds()),
                String.valueOf(properties.accountMaxAttempts()));
        return Long.valueOf(1L).equals(allowed);
    }

    /**
     * 计算不可逆的 Redis key 摘要。
     *
     * @param value 原始 IP 或账号
     * @return 十六进制 SHA-256 摘要
     */
    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 不支持 SHA-256", exception);
        }
    }
}
