package com.hengxue.auth.application.service;

import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.auth.interfaces.rest.response.UserResponse;

/** 用户注册应用服务。 */
public interface RegistrationService {

    /**
     * 注册用户并处理重复请求。
     *
     * @param command 注册命令
     * @param idempotencyKey 幂等键
     * @return 用户响应
     */
    UserResponse register(RegisterCommand command, String idempotencyKey);
}
