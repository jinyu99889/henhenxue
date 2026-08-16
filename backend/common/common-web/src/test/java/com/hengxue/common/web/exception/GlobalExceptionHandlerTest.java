package com.hengxue.common.web.exception;

import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.api.ApiResponse;
import com.hengxue.common.core.exception.BusinessException;
import com.hengxue.common.observability.TraceIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearTraceId() {
        TraceIdContext.clear();
    }

    @Test
    void mapsBusinessExceptionsToTheirContractStatus() {
        TraceIdContext.bind("trace-1");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(ApiErrorCode.RESOURCE_NOT_FOUND)
        );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("trace-1", response.getBody().traceId());
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        TraceIdContext.bind("trace-2");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(new IllegalStateException("数据库密码"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("系统繁忙，请稍后重试", response.getBody().message());
        assertEquals("trace-2", response.getBody().traceId());
    }

    @Test
    void mapsValidationAndUnreadableRequestBodiesToBadRequest() {
        TraceIdContext.bind("trace-3");

        ResponseEntity<ApiResponse<Void>> validation = handler.handleValidationException(null);
        ResponseEntity<ApiResponse<Void>> unreadable = handler.handleUnreadableMessage(null);

        assertFailure(validation, 400, "VALIDATION_ERROR", "trace-3");
        assertFailure(unreadable, 400, "VALIDATION_ERROR", "trace-3");
    }

    @Test
    void mapsMissingResourcesToNotFound() {
        TraceIdContext.bind("trace-4");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(null);

        assertFailure(response, 404, "RESOURCE_NOT_FOUND", "trace-4");
    }

    private void assertFailure(ResponseEntity<ApiResponse<Void>> response, int status, String code, String traceId) {
        assertEquals(status, response.getStatusCode().value());
        assertEquals(code, response.getBody().code());
        assertEquals(traceId, response.getBody().traceId());
    }
}
