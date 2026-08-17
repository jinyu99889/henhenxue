package com.hengxue.auth.domain.repository;

/** 登录请求的分布式限流端口。 */
public interface LoginRateLimitRepository {

    /**
     * 原子检查并消耗 IP 与账号维度的登录尝试次数。
     *
     * @param sourceIp 请求来源 IP
     * @param account 规范化后的用户名或邮箱，仓储会先计算摘要
     * @return 两个维度均未超限时为 {@code true}
     */
    boolean tryAcquire(String sourceIp, String account);
}
