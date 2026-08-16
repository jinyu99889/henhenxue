package com.hengxue.common.core.task;

import java.time.Instant;
import java.util.Objects;

/** {@code GET /tasks/{taskId}} 返回的公共异步任务状态。 */
public record AsyncTask(
        /** 异步任务的 ULID。 */
        String taskId,
        /** 任务的固定业务类型。 */
        TaskType taskType,
        /** 当前任务生命周期状态。 */
        TaskStatus status,
        /** 服务端报告的执行进度，范围为 0 至 100。 */
        int progress,
        /** 任务最终写入或影响的资源类型。 */
        String resourceType,
        /** 任务最终写入或影响的资源 ULID。 */
        String resourceId,
        /** 失败时返回的业务错误码；非失败状态可为 {@code null}。 */
        String errorCode,
        /** 已脱敏的失败说明；非失败状态可为 {@code null}。 */
        String errorMessage,
        /** 首次开始执行的 UTC 时间；尚未执行时可为 {@code null}。 */
        Instant startedAt,
        /** 任务进入最终状态的 UTC 时间；未结束时可为 {@code null}。 */
        Instant finishedAt
) {

    /**
     * 创建异步任务查询结果并校验公共字段范围。
     *
     * @param taskId 异步任务的 ULID
     * @param taskType 任务的固定业务类型
     * @param status 当前任务生命周期状态
     * @param progress 服务端报告的执行进度，范围为 0 至 100
     * @param resourceType 任务关联的资源类型
     * @param resourceId 任务关联的资源 ULID
     * @param errorCode 失败时的业务错误码，其他状态可为 {@code null}
     * @param errorMessage 已脱敏的失败说明，其他状态可为 {@code null}
     * @param startedAt 首次开始执行的 UTC 时间，尚未执行时可为 {@code null}
     * @param finishedAt 进入最终状态的 UTC 时间，未结束时可为 {@code null}
     */
    public AsyncTask {
        requireText(taskId, "任务 ID");
        Objects.requireNonNull(taskType, "任务类型不能为空");
        Objects.requireNonNull(status, "任务状态不能为空");
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("任务进度必须在 0 至 100 之间");
        }
        requireText(resourceType, "资源类型");
        requireText(resourceId, "资源 ID");
    }

    /**
     * 校验接口契约要求的非空文本字段。
     *
     * @param value 待校验的字段值
     * @param name 字段名称，用于构造异常信息
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空或空白");
        }
    }
}
