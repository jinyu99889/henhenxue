package com.hengxue.auth.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hengxue.auth.application.service.RegistrationIdempotencyService;
import com.hengxue.auth.application.support.IdempotencyStart;
import com.hengxue.auth.application.support.SnowflakeIdGenerator;
import com.hengxue.auth.infrastructure.persistence.entity.IdempotencyRecord;
import com.hengxue.auth.infrastructure.persistence.mapper.IdempotencyRecordMapper;
import com.hengxue.auth.interfaces.rest.response.UserResponse;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 管理匿名注册请求的持久化幂等记录。 */
@Service
public class RegistrationIdempotencyServiceImpl implements RegistrationIdempotencyService {

    static final String OWNER_SERVICE = "auth-service";
    static final String PUBLIC_REGISTRATION_ACTOR_ID = "01J00000000000000000000000";
    static final String ROUTE = "/auth/register";

    @Autowired
    private IdempotencyRecordMapper mapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建幂等记录，或读取已有请求的结果。
     *
     * @param key 客户端提交的 UUID 幂等键
     * @param requestHash 当前请求摘要
     * @return 首次请求或已完成请求的判定结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyStart begin(String key, String requestHash) {
        IdempotencyRecord existing = findRecord(key);
        if (existing != null) {
            return resolveExisting(existing, requestHash);
        }
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setId(snowflakeIdGenerator.next());
            record.setOwnerService(OWNER_SERVICE);
            record.setRequesterUserId(PUBLIC_REGISTRATION_ACTOR_ID);
            record.setRoute(ROUTE);
            record.setIdempotencyKey(key);
            record.setRequestHash(requestHash);
            record.setStatus("PROCESSING");
            record.setExpiresAt(LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(7));
            mapper.insert(record);
            return IdempotencyStart.started();
        } catch (DuplicateKeyException exception) {
            IdempotencyRecord concurrentRecord = findRecord(key);
            if (concurrentRecord == null) {
                throw exception;
            }
            return resolveExisting(concurrentRecord, requestHash);
        }
    }

    /**
     * 在注册事务中标记幂等记录完成。
     *
     * @param key 幂等键
     * @param user 新注册用户
     */
    @Override
    public void complete(String key, UserResponse user) {
        try {
            mapper.update(
                    null,
                    Wrappers.<IdempotencyRecord>lambdaUpdate()
                            .eq(IdempotencyRecord::getOwnerService, OWNER_SERVICE)
                            .eq(IdempotencyRecord::getRequesterUserId, PUBLIC_REGISTRATION_ACTOR_ID)
                            .eq(IdempotencyRecord::getRoute, ROUTE)
                            .eq(IdempotencyRecord::getIdempotencyKey, key)
                            .set(IdempotencyRecord::getStatus, "COMPLETED")
                            .set(IdempotencyRecord::getResourceType, "USER")
                            .set(IdempotencyRecord::getResourceId, user.id())
                            .set(IdempotencyRecord::getResponseStatus, 201)
                            .set(IdempotencyRecord::getResponseJson, objectMapper.writeValueAsString(user))
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化注册响应", exception);
        }
    }

    /**
     * 清理失败请求留下的进行中记录。
     *
     * @param key 幂等键
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void discard(String key) {
        mapper.delete(Wrappers.<IdempotencyRecord>lambdaQuery()
                .eq(IdempotencyRecord::getOwnerService, OWNER_SERVICE)
                .eq(IdempotencyRecord::getRequesterUserId, PUBLIC_REGISTRATION_ACTOR_ID)
                .eq(IdempotencyRecord::getRoute, ROUTE)
                .eq(IdempotencyRecord::getIdempotencyKey, key)
                .eq(IdempotencyRecord::getStatus, "PROCESSING"));
    }

    /**
     * 解析已有幂等记录。
     *
     * @param record 已有记录
     * @param requestHash 当前请求摘要
     * @return 已完成请求的用户结果或进行中异常
     */
    private IdempotencyStart resolveExisting(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ApiErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        if (!"COMPLETED".equals(record.getStatus()) || record.getResponseJson() == null) {
            throw new BusinessException(ApiErrorCode.REQUEST_IN_PROGRESS);
        }
        try {
            return IdempotencyStart.completed(objectMapper.readValue(record.getResponseJson(), UserResponse.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法读取注册幂等响应", exception);
        }
    }

    private IdempotencyRecord findRecord(String key) {
        return mapper.selectOne(Wrappers.<IdempotencyRecord>lambdaQuery()
                .eq(IdempotencyRecord::getOwnerService, OWNER_SERVICE)
                .eq(IdempotencyRecord::getRequesterUserId, PUBLIC_REGISTRATION_ACTOR_ID)
                .eq(IdempotencyRecord::getRoute, ROUTE)
                .eq(IdempotencyRecord::getIdempotencyKey, key)
                .last("LIMIT 1"));
    }
}
