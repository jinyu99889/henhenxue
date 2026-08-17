package com.hengxue.auth.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 登录接口的 Redis 限流参数。 */
@Validated
@ConfigurationProperties(prefix = "auth.login")
public record AuthLoginProperties(
        @Min(1) int ipWindowSeconds,
        @Min(1) int ipMaxAttempts,
        @Min(1) int accountWindowSeconds,
        @Min(1) int accountMaxAttempts
) {
}
