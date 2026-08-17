package com.hengxue.auth.interfaces.rest.support;

import com.hengxue.auth.config.TrustedProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 从可信网关转发头中解析用于限流的客户端 IP。 */
@Component
public class ClientIpResolver {

    @Autowired
    private TrustedProxyProperties properties;

    /**
     * 解析当前请求的限流 IP。
     *
     * @param request Servlet 请求
     * @return 首个可信代理转发地址，或直接连接地址
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!properties.isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddress;
        }
        return forwardedFor.split(",", 2)[0].strip();
    }
}
