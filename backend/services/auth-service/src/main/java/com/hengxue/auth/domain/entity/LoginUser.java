package com.hengxue.auth.domain.entity;

/** 登录校验所需的用户主体与密码身份快照。 */
public record LoginUser(
        String id,
        String username,
        String email,
        java.time.LocalDateTime emailVerifiedAt,
        String nickname,
        String passwordHash,
        int version
) {
}
