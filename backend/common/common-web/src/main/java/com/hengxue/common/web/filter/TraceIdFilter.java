package com.hengxue.common.web.filter;

import com.hengxue.common.observability.TraceIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在 Servlet 请求边界绑定、回写并清理 traceId 的 Filter。 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** 用于跨 HTTP 服务传递链路标识的请求头名称。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    /**
     * 绑定合法的上游 traceId，或在缺失、非法时生成新的标识。
     *
     * @param request 当前 Servlet 请求
     * @param response 当前 Servlet 响应
     * @param filterChain 后续 Filter 链
     * @throws ServletException Servlet 处理失败时抛出
     * @throws IOException 响应写入失败时抛出
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        TraceIdContext.bind(traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Servlet 容器会复用工作线程，必须在请求结束后清理上下文。
            TraceIdContext.clear();
        }
    }

    /**
     * 校验上游 traceId，并在不可用时生成新的链路标识。
     *
     * @param candidate 上游请求头中的 traceId
     * @return 合法的上游 traceId 或新生成的 traceId
     */
    private String resolveTraceId(String candidate) {
        if (candidate != null && TRACE_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
