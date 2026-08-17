package com.hengxue.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** auth-service 直连场景下的 Sa-Token 请求鉴权配置。 */
@Configuration
public class SaTokenWebMvcConfiguration implements WebMvcConfigurer {

    /**
     * 注册公开接口和登录接口之外的会话校验。
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter
                        .match("/auth/**")
                        .notMatch("/auth/login", "/auth/register", "/auth/email-codes")
                        .check(route -> StpUtil.checkLogin())))
                .addPathPatterns("/**");
    }
}
