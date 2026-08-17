package com.hengxue.auth.domain.entity;

/** 已完成输入规范化、待持久化的新用户信息。 */
public record NewUser(
        String id,
        String username,
        String email,
        String nickname
) {
}
