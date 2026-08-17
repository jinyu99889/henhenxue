package com.hengxue.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 注册邮件的发送人配置。 */
@Validated
@ConfigurationProperties(prefix = "hengxue.auth.mail")
public record AuthMailProperties(
        @NotBlank @Email String from
) {
}
