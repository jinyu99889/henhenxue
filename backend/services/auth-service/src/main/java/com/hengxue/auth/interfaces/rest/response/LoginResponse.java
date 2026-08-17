package com.hengxue.auth.interfaces.rest.response;

/** 登录成功后返回的 Sa-Token 会话和用户摘要。 */
public record LoginResponse(String token, UserResponse user) {
}
