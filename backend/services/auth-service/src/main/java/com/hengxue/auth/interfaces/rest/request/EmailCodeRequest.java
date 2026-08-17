package com.hengxue.auth.interfaces.rest.request;

import com.hengxue.auth.domain.enums.EmailCodePurpose;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发送邮箱验证码的 HTTP 请求体。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record EmailCodeRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull EmailCodePurpose purpose
) {
}
