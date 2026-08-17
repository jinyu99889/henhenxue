package com.hengxue.auth.application.support;

import com.hengxue.auth.application.command.RegisterCommand;
import java.nio.charset.StandardCharsets;
import com.hengxue.auth.config.EmailCodeProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 计算注册请求的 HMAC 幂等摘要，避免可离线猜测密码。 */
@Component
public class RegistrationRequestHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    @org.springframework.beans.factory.annotation.Autowired
    private EmailCodeProperties properties;

    /**
     * 计算固定字段顺序的 SHA-256 摘要。
     *
     * @param command 注册命令
     * @return 小写十六进制 HMAC-SHA-256 摘要
     */
    public String hash(RegisterCommand command) {
        String value = "registration-request\n" + command.username() + '\n' + command.email() + '\n' + command.emailCode() + '\n'
                + command.password() + '\n' + command.nickname();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.hmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(Character.forDigit((item >>> 4) & 0x0F, 16));
                builder.append(Character.forDigit(item & 0x0F, 16));
            }
            return builder.toString();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法初始化注册请求摘要器", exception);
        }
    }
}
