package com.hengxue.auth.interfaces.rest;

import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.auth.application.service.EmailCodeService;
import com.hengxue.auth.application.service.LoginService;
import com.hengxue.auth.application.service.RegistrationService;
import com.hengxue.auth.domain.enums.AuthErrorCode;
import com.hengxue.auth.domain.enums.EmailCodePurpose;
import com.hengxue.auth.interfaces.rest.request.EmailCodeRequest;
import com.hengxue.auth.interfaces.rest.request.LoginRequest;
import com.hengxue.auth.interfaces.rest.request.RegisterRequest;
import com.hengxue.auth.interfaces.rest.response.UserResponse;
import com.hengxue.auth.interfaces.rest.response.LoginResponse;
import com.hengxue.auth.interfaces.rest.support.ClientIpResolver;
import com.hengxue.common.core.api.ApiResponse;
import com.hengxue.common.core.exception.BusinessException;
import com.hengxue.common.observability.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 身份与注册公开 HTTP 接口。 */
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private EmailCodeService emailCodeApplicationService;

    @Autowired
    private RegistrationService registrationApplicationService;

    @Autowired
    private LoginService loginApplicationService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    /**
     * 发送注册邮箱验证码。
     *
     * @param request 客户端请求体
     * @param servletRequest Servlet 请求，用于服务端识别来源 IP
     * @return HTTP 202 的统一空响应
     */
    @PostMapping("/email-codes")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(
            @Valid @RequestBody EmailCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        if (request.purpose() != EmailCodePurpose.REGISTER) {
            throw new BusinessException(AuthErrorCode.EMAIL_CODE_PURPOSE_UNSUPPORTED);
        }
        emailCodeApplicationService.sendRegistrationCode(request.email(), clientIpResolver.resolve(servletRequest));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(null, TraceIdContext.getOrCreate()));
    }

    /**
     * 注册用户并绑定普通用户角色。
     *
     * @param request 客户端请求体
     * @param idempotencyKey UUID 格式幂等键
     * @return HTTP 201 的用户摘要和版本 ETag
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$") String idempotencyKey
    ) {
        UserResponse user = registrationApplicationService.register(
                new RegisterCommand(request.username(), request.email(), request.emailCode(), request.password(), request.nickname()),
                idempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag("\"" + user.version() + "\"")
                .body(ApiResponse.success(user, TraceIdContext.getOrCreate()));
    }

    /**
     * 使用用户名或邮箱登录。
     *
     * @param request 登录请求体
     * @param servletRequest Servlet 请求，用于服务端识别来源 IP
     * @return HTTP 200 的会话令牌和用户摘要
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        LoginResponse response = loginApplicationService.login(
                request.account(), request.password(), clientIpResolver.resolve(servletRequest));
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdContext.getOrCreate()));
    }

    /**
     * 注销当前请求对应的设备会话。
     *
     * @return HTTP 204 空响应
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        loginApplicationService.logout();
        return ResponseEntity.noContent().build();
    }
}
