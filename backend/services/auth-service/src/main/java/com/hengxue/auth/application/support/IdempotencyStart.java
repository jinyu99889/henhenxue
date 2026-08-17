package com.hengxue.auth.application.support;

import com.hengxue.auth.interfaces.rest.response.UserResponse;

/** 注册幂等记录创建后的状态。 */
public record IdempotencyStart(UserResponse completedUser) {

    /**
     * 创建首次请求状态。
     *
     * @return 尚未完成的首次请求状态
     */
    public static IdempotencyStart started() {
        return new IdempotencyStart(null);
    }

    /**
     * 创建已完成请求状态。
     *
     * @param user 首次请求创建的用户
     * @return 带用户结果的状态
     */
    public static IdempotencyStart completed(UserResponse user) {
        return new IdempotencyStart(user);
    }

    /**
     * 判断当前是否已有成功结果。
     *
     * @return 已完成时为 {@code true}
     */
    public boolean isCompleted() {
        return completedUser != null;
    }
}
