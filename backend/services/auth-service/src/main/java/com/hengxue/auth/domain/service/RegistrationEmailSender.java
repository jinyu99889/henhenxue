package com.hengxue.auth.domain.service;

/** 注册验证码邮件发送端口。 */
public interface RegistrationEmailSender {

    /**
     * 向邮箱发送注册验证码。
     *
     * @param email 收件邮箱
     * @param code 六位数字验证码
     */
    void sendRegistrationCode(String email, String code);
}
