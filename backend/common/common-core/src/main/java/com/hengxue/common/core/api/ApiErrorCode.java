package com.hengxue.common.core.api;

/**
 * v1 HTTP 契约定义的跨服务稳定业务错误码。
 *
 * <p>服务私有错误码应保留在所属服务中。HTTP 状态以整数表示，使核心模块无需依赖 Spring Web。</p>
 */
public enum ApiErrorCode implements BusinessErrorCode {
    VALIDATION_ERROR(400, "请求参数不合法"),
    UNAUTHENTICATED(401, "请先登录"),
    FORBIDDEN(403, "没有访问权限"),
    RESOURCE_NOT_FOUND(404, "资源不存在或不可访问"),
    VERSION_CONFLICT(409, "资源已被更新，请刷新后重试"),
    IDEMPOTENCY_KEY_REUSED(409, "幂等键不能用于不同的请求"),
    INVALID_STATE_TRANSITION(409, "当前状态不允许此操作"),
    TREE_NOT_READY(409, "知识树尚未准备完成"),
    REQUEST_IN_PROGRESS(409, "请求正在处理中"),
    LEARNING_NODE_NOT_LEAF(422, "仅叶子节点可发起追问"),
    IMPORT_ASSET_INVALID(422, "导入资源无效"),
    RATE_LIMITED(429, "请求过于频繁，请稍后重试"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用，请稍后重试");

    private final int httpStatus;
    private final String defaultMessage;

    /**
     * 创建业务错误码。
     *
     * @param httpStatus 此错误对应的 HTTP 语义状态码
     * @param defaultMessage 可直接展示给用户的默认提示，不得包含敏感信息
     */
    ApiErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 获取对外稳定的业务错误码。
     *
     * @return 业务错误码
     */
    @Override
    public String code() {
        return name();
    }

    /**
     * 获取错误对应的 HTTP 语义状态码。
     *
     * @return HTTP 状态码
     */
    @Override
    public int httpStatus() {
        return httpStatus;
    }

    /**
     * 获取可展示给用户的默认错误提示。
     *
     * @return 默认错误提示
     */
    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
