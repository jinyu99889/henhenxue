package com.hengxue.auth.application.support;

import com.hengxue.auth.config.EmailCodeProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 对短验证码进行 HMAC 签名，避免其明文落入 Redis。 */
@Component
public class VerificationCodeSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @org.springframework.beans.factory.annotation.Autowired
    private EmailCodeProperties properties;

    /**
     * 计算验证码签名。
     *
     * @param email 规范化后的邮箱
     * @param code 六位数字验证码
     * @return 固定长度的小写十六进制签名
     */
    public String sign(String email, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.hmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal((email + ':' + code).getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法初始化验证码签名器", exception);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 待编码的字节数组
     * @return 小写十六进制字符串
     */
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >>> 4) & 0x0F, 16));
            builder.append(Character.forDigit(value & 0x0F, 16));
        }
        return builder.toString();
    }
}
