package com.hengxue.auth.domain.repository;

/** 验证码、发送锁和限流状态的 Redis 端口。 */
public interface VerificationCodeRepository {

    /**
     * 判断某邮箱是否已有有效注册验证码。
     *
     * @param email 规范化后的邮箱
     * @return 剩余秒数；不存在时为零
     */
    long registrationCodeRemainingSeconds(String email);

    /**
     * 尝试获取邮箱发送锁。
     *
     * @param email 规范化后的邮箱
     * @return 成功获取时为 {@code true}
     */
    boolean acquireRegistrationSendLock(String email);

    /**
     * 释放当前邮箱的发送锁。
     *
     * @param email 规范化后的邮箱
     */
    void releaseRegistrationSendLock(String email);

    /**
     * 检查并增加来源 IP 的发送计数。
     *
     * @param sourceIp 客户端来源 IP
     * @return 未超出窗口限额时为 {@code true}
     */
    boolean tryAcquireSendQuota(String sourceIp);

    /**
     * 保存注册验证码签名。
     *
     * @param email 规范化后的邮箱
     * @param signedCode 验证码签名
     */
    void saveRegistrationCode(String email, String signedCode);

    /**
     * 在邮件发送失败时，仅删除匹配当前签名的验证码。
     *
     * @param email 规范化后的邮箱
     * @param signedCode 当前请求生成的验证码签名
     */
    void removeRegistrationCodeIfMatches(String email, String signedCode);

    /**
     * 验证并单次消费注册验证码。
     *
     * @param email 规范化后的邮箱
     * @param signedCode 调用方提交验证码的签名
     * @return 校验成功并完成消费时为 {@code true}
     */
    boolean consumeRegistrationCode(String email, String signedCode);
}
