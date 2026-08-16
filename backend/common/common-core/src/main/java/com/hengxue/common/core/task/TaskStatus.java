package com.hengxue.common.core.task;

/** 异步任务查询接口返回的任务生命周期状态。 */
public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
