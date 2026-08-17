package com.hengxue.auth.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 可被信任并允许传递客户端 IP 请求头的反向代理配置。 */
@ConfigurationProperties(prefix = "hengxue.auth")
public record TrustedProxyProperties(Set<String> trustedProxyIps) {

    /**
     * 判断连接地址是否为可信反向代理。
     *
     * @param remoteAddress TCP 连接对端地址
     * @return 可读取代理请求头时为 {@code true}
     */
    public boolean isTrustedProxy(String remoteAddress) {
        return trustedProxyIps != null && trustedProxyIps.contains(remoteAddress);
    }
}
