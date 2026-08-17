package com.hengxue.auth.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 已持久化的注册幂等记录。 */
@TableName("sys_idempotency_record")
public class IdempotencyRecord {

    @TableId(type = IdType.INPUT)
    private String id;
    private String ownerService;
    private String requesterUserId;
    private String route;
    private String idempotencyKey;
    private String requestHash;
    private String status;
    private String resourceType;
    private String resourceId;
    private Integer responseStatus;
    private String responseJson;
    private LocalDateTime expiresAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerService() { return ownerService; }
    public void setOwnerService(String ownerService) { this.ownerService = ownerService; }
    public String getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(String requesterUserId) { this.requesterUserId = requesterUserId; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    /**
     * 获取请求摘要。
     *
     * @return 请求摘要
     */
    public String getRequestHash() {
        return requestHash;
    }

    /**
     * 设置请求摘要。
     *
     * @param requestHash 请求摘要
     */
    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    /**
     * 获取执行状态。
     *
     * @return 执行状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置执行状态。
     *
     * @param status 执行状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取首次成功响应的用户 JSON。
     *
     * @return 用户 JSON
     */
    public String getResponseJson() {
        return responseJson;
    }

    /**
     * 设置首次成功响应的用户 JSON。
     *
     * @param responseJson 用户 JSON
     */
    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
