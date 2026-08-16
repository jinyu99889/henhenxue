package com.hengxue.common.web.autoconfigure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.hengxue.common.web.filter.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonWebAutoConfigurationTest {

    private final CommonWebAutoConfiguration configuration = new CommonWebAutoConfiguration();

    @Test
    void registersTheTraceIdFilterAtTheHighestPrecedence() {
        FilterRegistrationBean<TraceIdFilter> registration = configuration.traceIdFilterRegistration();

        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
        assertInstanceOf(TraceIdFilter.class, registration.getFilter());
    }

    @Test
    void configuresStrictJsonDeserializationAndUtcTimezone() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        configuration.commonWebJacksonCustomizer().customize(builder);

        assertTrue(builder.build().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertEquals("UTC", builder.build().getDeserializationConfig().getTimeZone().getID());
    }
}
