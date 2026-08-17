package com.hengxue.auth.application.service;

import com.hengxue.auth.application.support.IdempotencyStart;
import com.hengxue.auth.interfaces.rest.response.UserResponse;

/** 注册幂等服务。 */
public interface RegistrationIdempotencyService {

    IdempotencyStart begin(String key, String requestHash);
    void complete(String key, UserResponse user);
    void discard(String key);
}
