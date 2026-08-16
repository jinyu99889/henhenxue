package com.hengxue.common.core.exception;

import com.hengxue.common.core.api.ApiErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessExceptionTest {

    @Test
    void usesTheErrorCodesDefaultMessage() {
        BusinessException exception = new BusinessException(ApiErrorCode.RESOURCE_NOT_FOUND);

        assertSame(ApiErrorCode.RESOURCE_NOT_FOUND, exception.errorCode());
        assertEquals("资源不存在或不可访问", exception.getMessage());
    }

    @Test
    void preservesTheSafeCustomMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("内部原因");
        BusinessException exception = new BusinessException(ApiErrorCode.VERSION_CONFLICT, "资源已更新", cause);

        assertEquals("资源已更新", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void rejectsMissingErrorCodesAndBlankMessages() {
        assertEquals("业务错误码不能为空", assertThrows(
                NullPointerException.class,
                () -> new BusinessException(null, "提示")
        ).getMessage());
        assertEquals("业务异常提示不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> new BusinessException(ApiErrorCode.FORBIDDEN, " ")
        ).getMessage());
        assertEquals("业务异常提示不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> new BusinessException(ApiErrorCode.FORBIDDEN, null)
        ).getMessage());
    }
}
