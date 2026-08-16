package com.hengxue.common.web.autoconfigure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.hengxue.common.web.exception.GlobalExceptionHandler;
import com.hengxue.common.web.filter.TraceIdFilter;
import com.hengxue.common.web.mvc.WebMvcConfiguration;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/** Servlet 服务共用的 Web 自动配置入口。 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class})
public class CommonWebAutoConfiguration {

    /**
     * 注册 traceId Filter，使异常处理和业务日志可获得相同的链路标识。
     *
     * @return 以最高优先级执行的 traceId Filter 注册信息
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 统一 JSON 请求体的严格反序列化与 UTC 时间处理。
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer commonWebJacksonCustomizer() {
        return builder -> {
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.timeZone(TimeZone.getTimeZone("UTC"));
        };
    }
}
