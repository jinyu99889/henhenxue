package com.hengxue.gateway;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 网关统一执行 Sa-Token 会话校验。 */
@Configuration
public class SaTokenGatewayConfiguration {

    /**
     * 注册 WebFlux 全局鉴权过滤器。
     *
     * @return Sa-Token Reactor 过滤器
     */
    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/api/v1/**")
                .addExclude(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/email-codes",
                        "/actuator/**")
                .setAuth(obj -> SaRouter.match("/api/v1/**").check(r -> StpUtil.checkLogin()))
                .setError(exception -> SaResult.error("请先登录").setCode(401));
    }
}
