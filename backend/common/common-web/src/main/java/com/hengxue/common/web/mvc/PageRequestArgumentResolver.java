package com.hengxue.common.web.mvc;

import com.hengxue.common.core.api.ApiErrorCode;
import com.hengxue.common.core.api.PageRequest;
import com.hengxue.common.core.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 将 HTTP 查询参数统一解析为 {@link PageRequest} 的 MVC 参数解析器。 */
public class PageRequestArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String PAGE_PARAMETER = "page";
    private static final String PAGE_SIZE_PARAMETER = "pageSize";

    /**
     * 判断 Controller 参数是否需要按通用分页规则解析。
     *
     * @param parameter Controller 方法参数元数据
     * @return 参数类型为 {@link PageRequest} 时返回 true
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PageRequest.class.equals(parameter.getParameterType());
    }

    /**
     * 读取 page 和 pageSize 查询参数并构造分页请求。
     *
     * @param parameter Controller 方法参数元数据
     * @param mavContainer 当前模型与视图容器，不参与分页解析
     * @param webRequest 当前原生 Web 请求
     * @param binderFactory 数据绑定工厂，不参与分页解析
     * @return 已校验的分页请求
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        int page = parseParameter(webRequest.getParameter(PAGE_PARAMETER), 1, "页码");
        int pageSize = parseParameter(webRequest.getParameter(PAGE_SIZE_PARAMETER), PageRequest.DEFAULT_PAGE_SIZE, "每页数量");
        try {
            return new PageRequest(page, pageSize);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ApiErrorCode.VALIDATION_ERROR, exception.getMessage(), exception);
        }
    }

    /**
     * 解析整数查询参数，缺失时使用约定默认值。
     *
     * @param value 查询参数原始值
     * @param defaultValue 参数缺失时使用的默认值
     * @param displayName 用于构造中文错误提示的参数显示名称
     * @return 已解析的整数值
     */
    private int parseParameter(String value, int defaultValue, String displayName) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ApiErrorCode.VALIDATION_ERROR, displayName + "必须是整数", exception);
        }
    }
}
