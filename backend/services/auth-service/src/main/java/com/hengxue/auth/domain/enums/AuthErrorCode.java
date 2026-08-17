package com.hengxue.auth.domain.enums;

import com.hengxue.common.core.api.BusinessErrorCode;

/** 身份服务对外使用的业务错误码。 */
public enum AuthErrorCode implements BusinessErrorCode {
    USERNAME_ALREADY_EXISTS(409, "用户名已被使用"),
    EMAIL_ALREADY_REGISTERED(409, "邮箱已完成注册"),
    EMAIL_CODE_INVALID(400, "邮箱验证码无效或已过期"),
    EMAIL_CODE_PURPOSE_UNSUPPORTED(400, "暂不支持该验证码用途"),
    USER_ROLE_UNAVAILABLE(503, "用户默认角色不可用");

    private final int httpStatus;
    private final String defaultMessage;

    /**
     * 创建业务错误码。
     *
     * @param httpStatus 对应的 HTTP 状态码
     * @param defaultMessage 可安全展示给调用方的默认提示
     */
    AuthErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 获取稳定的错误码。
     *
     * @return 枚举名称形式的错误码
     */
    @Override
    public String code() {
        return name();
    }

    /**
     * 获取对应的 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    @Override
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * 获取默认错误提示。
     *
     * @return 面向用户的中文提示
     */
    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
