package com.hengxue.common.core.task;

import java.util.Objects;

/** 长任务被受理后返回的响应数据载荷。 */
public record TaskAccepted(
        /** 异步任务的 ULID。 */
        String taskId,
        /** 受理响应固定为 {@link TaskStatus#PENDING}。 */
        TaskStatus status,
        /** 任务最终写入或影响的资源类型。 */
        String resourceType,
        /** 任务最终写入或影响的资源 ULID。 */
        String resourceId
) {

    /**
     * 创建任务受理响应并确保其状态符合契约。
     *
     * @param taskId 异步任务的 ULID
     * @param status 受理响应状态，只允许 PENDING
     * @param resourceType 任务关联的资源类型
     * @param resourceId 任务关联的资源 ULID
     */
    public TaskAccepted {
        requireText(taskId, "任务 ID");
        Objects.requireNonNull(status, "任务状态不能为空");
        if (status != TaskStatus.PENDING) {
            throw new IllegalArgumentException("任务受理响应的状态必须为 PENDING");
        }
        requireText(resourceType, "资源类型");
        requireText(resourceId, "资源 ID");
    }

    /**
     * 创建状态固定为 PENDING 的任务受理响应。
     *
     * @param taskId 异步任务的 ULID
     * @param resourceType 任务关联的资源类型
     * @param resourceId 任务关联的资源 ULID
     * @return 状态为 PENDING 的受理响应
     */
    public static TaskAccepted pending(String taskId, String resourceType, String resourceId) {
        return new TaskAccepted(taskId, TaskStatus.PENDING, resourceType, resourceId);
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
