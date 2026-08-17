package com.hengxue.auth.interfaces.rest.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 用户注册的 HTTP 请求体。 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RegisterRequest(
        @NotBlank @Size(min = 8, max = 64) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String emailCode,
        @NotBlank @Size(min = 8, max = 128) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String password,
        @NotBlank @Size(min = 1, max = 64) String nickname
) {
}
