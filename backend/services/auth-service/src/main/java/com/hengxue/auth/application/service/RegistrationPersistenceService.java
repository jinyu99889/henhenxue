package com.hengxue.auth.application.service;

import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.auth.interfaces.rest.response.UserResponse;

/** 用户注册数据库事务服务。 */
public interface RegistrationPersistenceService {

    /**
     * 在数据库事务中创建用户、身份和角色关联。
     *
     * @param command 注册命令
     * @param idempotencyKey 幂等键
     * @return 用户响应
     */
    UserResponse create(RegisterCommand command, String idempotencyKey);
}
