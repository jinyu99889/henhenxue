package com.hengxue.auth.infrastructure.messaging;

import com.hengxue.auth.config.AuthMailProperties;
import com.hengxue.auth.domain.service.RegistrationEmailSender;
import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.exception.BusinessException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 使用 SMTP 发送注册验证码邮件的适配器。 */
@Component
public class SmtpRegistrationEmailSender implements RegistrationEmailSender {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuthMailProperties mailProperties;

    /**
     * 发送注册验证码邮件。
     *
     * @param email 收件邮箱
     * @param code 六位数字验证码
     */
    @Override
    public void sendRegistrationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(email);
        message.setSubject("狠狠学用户账号注册验证码");
        message.setText("您好，您正在进行狠狠学用户账号注册验证。您的账号所需验证码为："
                + code + "，验证码 10 分钟内有效。");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ApiErrorCode.SERVICE_UNAVAILABLE, "验证码邮件发送失败，请稍后重试", exception);
        }
    }
}
