package com.hengxue.common.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceIdContextTest {

    @AfterEach
    void clearTraceId() {
        TraceIdContext.clear();
    }

    @Test
    void returnsTheBoundTraceId() {
        TraceIdContext.bind("trace-1");

        assertEquals("trace-1", TraceIdContext.getOrCreate());
        assertEquals("trace-1", MDC.get("traceId"));
    }

    @Test
    void createsANewTraceIdAfterClearingTheContext() {
        String firstTraceId = TraceIdContext.getOrCreate();
        TraceIdContext.clear();

        assertNotEquals(firstTraceId, TraceIdContext.getOrCreate());
    }

    @Test
    void rejectsBlankTraceIdsAndClearsTheCurrentValue() {
        assertEquals("链路标识不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> TraceIdContext.bind(" ")
        ).getMessage());
        assertEquals("链路标识不能为空或空白", assertThrows(
                IllegalArgumentException.class,
                () -> TraceIdContext.bind(null)
        ).getMessage());

        TraceIdContext.bind("trace-1");
        TraceIdContext.clear();

        assertNull(TraceIdContext.currentTraceId());
        assertNull(MDC.get("traceId"));
    }
}
