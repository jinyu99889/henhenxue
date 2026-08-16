package com.hengxue.common.core.api;

import java.util.Objects;

/**
 * v1 HTTP 通用响应包装。
 *
 * <p>契约规定成功时 {@code code} 为数值 {@code 0}，失败时为业务错误码字符串，因此该字段使用 {@link Object} 表示。</p>
 */
public record ApiResponse<T>(
        /** 成功时为数值 0，失败时为业务错误码字符串。 */
        Object code,
        /** 面向调用方的结果说明。 */
        String message,
        /** 成功响应的数据载荷；失败时固定为 {@code null}。 */
        T data,
        /** 用于跨服务排查的请求链路标识。 */
        String traceId
) {

    public static final int SUCCESS_CODE = 0;
    public static final String SUCCESS_MESSAGE = "OK";

    /**
     * 创建通用响应包装。
     *
     * @param code 成功数值码或失败业务错误码
     * @param message 面向调用方的结果说明
     * @param data 成功响应的数据载荷；失败时为 {@code null}
     * @param traceId 请求链路标识
     */
    public ApiResponse {
        Objects.requireNonNull(code, "响应码不能为空");
        Objects.requireNonNull(message, "响应消息不能为空");
        Objects.requireNonNull(traceId, "链路标识不能为空");
    }

    /**
     * 创建成功响应。
     *
     * @param data 返回给调用方的数据载荷，可为 {@code null}
     * @param traceId 请求链路标识
     * @param <T> 数据载荷类型
     * @return code 为数值 0、message 为 OK 的响应
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, traceId);
    }

    /**
     * 使用错误码的默认提示创建失败响应。
     *
     * @param errorCode 跨服务稳定业务错误码
     * @param traceId 请求链路标识
     * @return data 为 {@code null} 的失败响应
     */
    public static ApiResponse<Void> failure(BusinessErrorCode errorCode, String traceId) {
        BusinessErrorCode checkedErrorCode = Objects.requireNonNull(errorCode, "业务错误码不能为空");
        return failure(checkedErrorCode, checkedErrorCode.defaultMessage(), traceId);
    }

    /**
     * 使用自定义提示创建失败响应。
     *
     * @param errorCode 跨服务稳定业务错误码
     * @param message 可安全展示给调用方的错误提示
     * @param traceId 请求链路标识
     * @return data 为 {@code null} 的失败响应
     */
    public static ApiResponse<Void> failure(BusinessErrorCode errorCode, String message, String traceId) {
        Objects.requireNonNull(errorCode, "业务错误码不能为空");
        return new ApiResponse<>(errorCode.code(), message, null, traceId);
    }
}
