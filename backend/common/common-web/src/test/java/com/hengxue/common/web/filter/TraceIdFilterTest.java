package com.hengxue.common.web.filter;

import com.hengxue.common.observability.TraceIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearTraceId() {
        TraceIdContext.clear();
    }

    @Test
    void propagatesTheIncomingTraceIdAndClearsTheContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-1");

        filter.doFilter(request, response, (currentRequest, currentResponse) ->
                assertEquals("trace-1", TraceIdContext.getOrCreate())
        );

        assertEquals("trace-1", response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
        assertNull(TraceIdContext.currentTraceId());
    }

    @Test
    void replacesAnIllegalIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "非法 空格");

        filter.doFilter(request, response, (currentRequest, currentResponse) -> {
        });

        assertFalse(response.getHeader(TraceIdFilter.TRACE_ID_HEADER).isBlank());
    }

    @Test
    void generatesATraceIdWhenTheHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (currentRequest, currentResponse) ->
                assertEquals(response.getHeader(TraceIdFilter.TRACE_ID_HEADER), TraceIdContext.currentTraceId())
        );

        assertFalse(response.getHeader(TraceIdFilter.TRACE_ID_HEADER).isBlank());
        assertNull(TraceIdContext.currentTraceId());
    }

    @Test
    void clearsTheTraceContextWhenTheChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, (currentRequest, currentResponse) -> {
            throw new RuntimeException("链路异常");
        }));

        assertNull(TraceIdContext.currentTraceId());
    }
}
