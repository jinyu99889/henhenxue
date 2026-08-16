package com.hengxue.common.core.api;

/**
 * 可映射为 HTTP 响应的业务错误码约定。
 *
 * <p>公共错误码与服务私有错误码均可实现此接口，避免将服务私有错误码写入跨服务枚举。</p>
 */
public interface BusinessErrorCode {

    /**
     * 获取对外稳定的业务错误码。
     *
     * @return 业务错误码
     */
    String code();

    /**
     * 获取错误对应的 HTTP 语义状态码。
     *
     * @return HTTP 状态码
     */
    int httpStatus();

    /**
     * 获取可安全展示给调用方的默认提示。
     *
     * @return 默认错误提示
     */
    String defaultMessage();
}
