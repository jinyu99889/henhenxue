package com.hengxue.auth.application.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.hengxue.auth.application.command.RegisterCommand;
import com.hengxue.auth.application.service.RegistrationIdempotencyService;
import com.hengxue.auth.application.service.RegistrationPersistenceService;
import com.hengxue.auth.application.service.RegistrationService;
import com.hengxue.auth.application.support.IdempotencyStart;
import com.hengxue.auth.application.support.RegistrationRequestHasher;
import com.hengxue.auth.interfaces.rest.response.UserResponse;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 注册用例的幂等编排入口。 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final String REGISTER_RESOURCE = "auth.registration.submit";

    @Autowired
    private RegistrationRequestHasher requestHasher;

    @Autowired
    private RegistrationIdempotencyService idempotencyService;

    @Autowired
    private RegistrationPersistenceService transactionService;

    /**
     * 执行可重复提交的用户注册。
     *
     * @param command 注册命令
     * @param idempotencyKey 客户端 UUID 幂等键
     * @return 首次或重复请求对应的用户摘要
     */
    @Override
    @SentinelResource(value = REGISTER_RESOURCE, blockHandler = "handleRegistrationBlocked")
    public UserResponse register(RegisterCommand command, String idempotencyKey) {
        IdempotencyStart start = idempotencyService.begin(idempotencyKey, requestHasher.hash(command));
        if (start.isCompleted()) {
            return start.completedUser();
        }
        try {
            return transactionService.create(command, idempotencyKey);
        } catch (RuntimeException exception) {
            idempotencyService.discard(idempotencyKey);
            throw exception;
        }
    }

    /**
     * 处理 Sentinel 对注册资源实施的流控。
     *
     * @param command 经接口校验后的注册命令
     * @param idempotencyKey 客户端 UUID 幂等键
     * @param exception Sentinel 流控异常
     * @return 此方法始终抛出业务异常
     */
    public UserResponse handleRegistrationBlocked(
            RegisterCommand command,
            String idempotencyKey,
            BlockException exception
    ) {
        throw new BusinessException(ApiErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试", exception);
    }
}
