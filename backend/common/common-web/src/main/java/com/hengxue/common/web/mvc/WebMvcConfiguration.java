package com.hengxue.common.web.mvc;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册 Servlet MVC 共用参数解析能力的配置。 */
@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer {

    /**
     * 注册通用分页参数解析器。
     *
     * @param resolvers Spring MVC 待注册的参数解析器列表
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageRequestArgumentResolver());
    }
}
