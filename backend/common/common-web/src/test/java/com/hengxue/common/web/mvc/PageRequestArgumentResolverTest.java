package com.hengxue.common.web.mvc;

import com.hengxue.common.core.api.PageRequest;
import com.hengxue.common.core.exception.BusinessException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestArgumentResolverTest {

    private final PageRequestArgumentResolver resolver = new PageRequestArgumentResolver();

    @Test
    void resolvesTheDocumentedDefaultPagination() throws Exception {
        PageRequest pageRequest = resolve(new MockHttpServletRequest());

        assertEquals(PageRequest.firstPage(), pageRequest);
    }

    @Test
    void rejectsANonNumericPageParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("page", "第一页");

        BusinessException exception = assertThrows(BusinessException.class, () -> resolve(request));

        assertEquals("VALIDATION_ERROR", exception.errorCode().code());
        assertEquals("页码必须是整数", exception.getMessage());
    }

    @Test
    void resolvesExplicitPaginationAndTreatsBlankValuesAsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("page", "3");
        request.addParameter("pageSize", "50");

        assertEquals(new PageRequest(3, 50), resolve(request));

        MockHttpServletRequest blankRequest = new MockHttpServletRequest();
        blankRequest.addParameter("page", " ");
        blankRequest.addParameter("pageSize", "");
        assertEquals(PageRequest.firstPage(), resolve(blankRequest));
    }

    @Test
    void mapsOutOfRangePaginationToAValidationBusinessException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("pageSize", "101");

        BusinessException exception = assertThrows(BusinessException.class, () -> resolve(request));

        assertEquals("VALIDATION_ERROR", exception.errorCode().code());
        assertEquals("每页数量必须在 1 至 100 之间", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void supportsOnlyPageRequestParameters() throws Exception {
        Method pageRequestMethod = SampleController.class.getDeclaredMethod("list", PageRequest.class);
        Method stringMethod = SampleController.class.getDeclaredMethod("find", String.class);

        assertTrue(resolver.supportsParameter(new MethodParameter(pageRequestMethod, 0)));
        assertFalse(resolver.supportsParameter(new MethodParameter(stringMethod, 0)));
    }

    private PageRequest resolve(MockHttpServletRequest request) throws Exception {
        Method method = SampleController.class.getDeclaredMethod("list", PageRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        return (PageRequest) resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
    }

    private static final class SampleController {

        private void list(PageRequest pageRequest) {
        }

        private void find(String keyword) {
        }
    }
}
