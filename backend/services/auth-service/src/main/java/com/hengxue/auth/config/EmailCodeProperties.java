package com.hengxue.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 邮箱验证码的有效期、限流和签名配置。 */
@Validated
@ConfigurationProperties(prefix = "hengxue.auth.email-code")
public record EmailCodeProperties(
        @NotBlank @Size(min = 32) String hmacSecret,
        @Min(60) int validitySeconds,
        @Min(10) int sendLockSeconds,
        @Min(60) int ipWindowSeconds,
        @Min(1) int ipMaxSends,
        @Min(1) int maxVerifyAttempts
) {
}
