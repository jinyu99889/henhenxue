package com.hengxue.auth.application.service;

/** 邮箱验证码应用服务。 */
public interface EmailCodeService {

    /**
     * 发送注册验证码。
     *
     * @param email 注册邮箱
     * @param sourceIp 客户端 IP
     */
    void sendRegistrationCode(String email, String sourceIp);
}
