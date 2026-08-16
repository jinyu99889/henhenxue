package com.hengxue.common.web.mvc;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WebMvcConfigurationTest {

    @Test
    void registersTheCommonPageRequestResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        new WebMvcConfiguration().addArgumentResolvers(resolvers);

        assertEquals(1, resolvers.size());
        assertInstanceOf(PageRequestArgumentResolver.class, resolvers.get(0));
    }
}
