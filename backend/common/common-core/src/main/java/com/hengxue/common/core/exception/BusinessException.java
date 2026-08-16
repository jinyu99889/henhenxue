package com.hengxue.common.core.exception;

import com.hengxue.common.core.api.BusinessErrorCode;
import java.util.Objects;

/**
 * 由业务或应用层主动抛出的可预期异常。
 *
 * <p>异常消息可被 HTTP 适配层返回给调用方，因此调用方传入的消息必须已脱敏且可公开展示。</p>
 */
public final class BusinessException extends RuntimeException {

    private final BusinessErrorCode errorCode;

    /**
     * 使用错误码的默认提示创建业务异常。
     *
     * @param errorCode 可映射为 HTTP 响应的业务错误码
     */
    public BusinessException(BusinessErrorCode errorCode) {
        this(errorCode, requireErrorCode(errorCode).defaultMessage());
    }

    /**
     * 使用已脱敏的自定义提示创建业务异常。
     *
     * @param errorCode 可映射为 HTTP 响应的业务错误码
     * @param message 可安全展示给调用方的中文提示
     */
    public BusinessException(BusinessErrorCode errorCode, String message) {
        super(requireMessage(message));
        this.errorCode = requireErrorCode(errorCode);
    }

    /**
     * 使用已脱敏提示和原始异常原因创建业务异常。
     *
     * @param errorCode 可映射为 HTTP 响应的业务错误码
     * @param message 可安全展示给调用方的中文提示
     * @param cause 仅用于日志排查的原始异常原因，不得直接返回给调用方
     */
    public BusinessException(BusinessErrorCode errorCode, String message, Throwable cause) {
        super(requireMessage(message), cause);
        this.errorCode = requireErrorCode(errorCode);
    }

    /**
     * 获取业务错误码。
     *
     * @return 可映射为 HTTP 响应的业务错误码
     */
    public BusinessErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 校验业务错误码不能为空。
     *
     * @param errorCode 待校验的业务错误码
     * @return 已校验的业务错误码
     */
    private static BusinessErrorCode requireErrorCode(BusinessErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "业务错误码不能为空");
    }

    /**
     * 校验可返回给调用方的提示不能为空或空白。
     *
     * @param message 待校验的错误提示
     * @return 已校验的错误提示
     */
    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("业务异常提示不能为空或空白");
        }
        return message;
    }
}
