package com.hengxue.common.core.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void createsTheContractSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.success("payload", "trace-1");

        assertEquals(0, response.code());
        assertEquals("OK", response.message());
        assertEquals("payload", response.data());
        assertEquals("trace-1", response.traceId());
    }

    @Test
    void createsAnErrorEnvelopeWithNoData() {
        ApiResponse<Void> response = ApiResponse.failure(ApiErrorCode.RATE_LIMITED, "trace-1");

        assertEquals("RATE_LIMITED", response.code());
        assertEquals("请求过于频繁，请稍后重试", response.message());
        assertNull(response.data());
    }

    @Test
    void preservesACustomSafeFailureMessage() {
        ApiResponse<Void> response = ApiResponse.failure(ApiErrorCode.FORBIDDEN, "自定义提示", "trace-1");

        assertEquals("FORBIDDEN", response.code());
        assertEquals("自定义提示", response.message());
        assertNull(response.data());
        assertEquals("trace-1", response.traceId());
    }

    @Test
    void serializesSuccessAndErrorCodesWithTheirContractTypes() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode success = objectMapper.readTree(objectMapper.writeValueAsString(ApiResponse.success("payload", "trace-1")));
        JsonNode failure = objectMapper.readTree(objectMapper.writeValueAsString(ApiResponse.failure(ApiErrorCode.FORBIDDEN, "trace-1")));

        assertEquals(0, success.path("code").intValue());
        assertEquals("OK", success.path("message").textValue());
        assertEquals("FORBIDDEN", failure.path("code").textValue());
        assertTrue(failure.path("data").isNull());
    }

    @Test
    void rejectsAMissingBusinessErrorCodeWithAChineseMessage() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ApiResponse.failure(null, "trace-1")
        );

        assertEquals("业务错误码不能为空", exception.getMessage());
    }

    @Test
    void rejectsMissingRequiredEnvelopeFields() {
        assertEquals("响应码不能为空", assertThrows(
                NullPointerException.class,
                () -> new ApiResponse<>(null, "OK", null, "trace-1")
        ).getMessage());
        assertEquals("响应消息不能为空", assertThrows(
                NullPointerException.class,
                () -> new ApiResponse<>(0, null, null, "trace-1")
        ).getMessage());
        assertEquals("链路标识不能为空", assertThrows(
                NullPointerException.class,
                () -> new ApiResponse<>(0, "OK", null, null)
        ).getMessage());
    }
}
