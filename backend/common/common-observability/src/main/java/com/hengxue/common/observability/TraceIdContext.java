package com.hengxue.common.observability;

import java.util.UUID;

/**
 * 当前线程的 traceId 上下文。
 *
 * <p>HTTP、Dubbo、消息和任务适配器均可在进入边界时绑定 traceId，并在退出时清理，避免线程复用造成链路串扰。</p>
 */
public final class TraceIdContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private TraceIdContext() {
    }

    /**
     * 获取当前线程已绑定的 traceId。
     *
     * @return 当前线程的 traceId；尚未绑定时返回 {@code null}
     */
    public static String currentTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 获取当前 traceId；当前线程未绑定时生成一个新的标识。
     *
     * @return 当前线程的 traceId
     */
    public static String getOrCreate() {
        String traceId = currentTraceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            TRACE_ID_HOLDER.set(traceId);
        }
        return traceId;
    }

    /**
     * 为当前线程绑定 traceId。
     *
     * @param traceId 要绑定的非空 traceId
     */
    public static void bind(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("链路标识不能为空或空白");
        }
        TRACE_ID_HOLDER.set(traceId);
    }

    /**
     * 清理当前线程的 traceId，在线程复用前必须调用。
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}
