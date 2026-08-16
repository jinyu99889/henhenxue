package com.hengxue.common.web.exception;

import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.api.ApiResponse;
import com.hengxue.common.core.api.BusinessErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import com.hengxue.common.observability.TraceIdContext;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 将 Servlet MVC 异常转换为统一 HTTP 响应的全局处理器。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务或应用层主动抛出的可预期异常。
     *
     * @param exception 包含安全业务错误码与提示的异常
     * @return 与业务错误码对应的统一 HTTP 响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return failure(exception.errorCode(), exception.getMessage());
    }

    /**
     * 处理请求参数、校验和绑定失败。
     *
     * @param exception 参数校验、类型转换或必填参数异常
     * @return HTTP 400 的统一参数校验失败响应
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        return failure(ApiErrorCode.VALIDATION_ERROR, ApiErrorCode.VALIDATION_ERROR.defaultMessage());
    }

    /**
     * 处理无法解析的 JSON 请求体。
     *
     * @param exception JSON 格式、字段类型或未知字段导致的解析异常
     * @return HTTP 400 的统一参数校验失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return failure(ApiErrorCode.VALIDATION_ERROR, ApiErrorCode.VALIDATION_ERROR.defaultMessage());
    }

    /**
     * 处理不存在的静态资源或未匹配路由。
     *
     * @param exception Spring MVC 抛出的资源不存在异常
     * @return HTTP 404 的统一资源不存在响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(NoResourceFoundException exception) {
        return failure(ApiErrorCode.RESOURCE_NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND.defaultMessage());
    }

    /**
     * 处理未被其他处理器捕获的异常。
     *
     * @param exception 未预期的服务端异常，仅记录类型和链路标识而不暴露给调用方
     * @return HTTP 500 的统一内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        String traceId = TraceIdContext.getOrCreate();
        // 异常消息和堆栈可能包含密钥等敏感信息，只记录类型和链路标识。
        LOGGER.error("未处理的接口异常，traceId={}，异常类型={}", traceId, exception.getClass().getName());
        return failure(ApiErrorCode.INTERNAL_ERROR, ApiErrorCode.INTERNAL_ERROR.defaultMessage(), traceId);
    }

    /**
     * 根据业务错误码构造统一失败响应。
     *
     * @param errorCode 业务错误码及其对应的 HTTP 状态码
     * @param message 可安全展示给调用方的中文错误提示
     * @return 包含当前 traceId 的统一失败响应
     */
    private ResponseEntity<ApiResponse<Void>> failure(BusinessErrorCode errorCode, String message) {
        return failure(errorCode, message, TraceIdContext.getOrCreate());
    }

    /**
     * 根据业务错误码、提示与 traceId 构造统一失败响应。
     *
     * @param errorCode 业务错误码及其对应的 HTTP 状态码
     * @param message 可安全展示给调用方的中文错误提示
     * @param traceId 当前请求的链路标识
     * @return 指定 HTTP 状态的统一失败响应
     */
    private ResponseEntity<ApiResponse<Void>> failure(BusinessErrorCode errorCode, String message, String traceId) {
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.failure(errorCode, message, traceId));
    }
}
