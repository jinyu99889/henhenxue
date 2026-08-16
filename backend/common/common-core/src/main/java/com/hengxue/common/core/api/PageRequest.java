package com.hengxue.common.core.api;

/** v1 列表接口共用的 1-based 分页请求约束。 */
public record PageRequest(
        /** 从 1 开始的页码。 */
        int page,
        /** 每页数量，取值范围为 1 至 100。 */
        int pageSize
) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 创建分页请求并校验页码范围。
     *
     * @param page 从 1 开始的页码
     * @param pageSize 每页数量，最大为 100
     */
    public PageRequest {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于或等于 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在 1 至 " + MAX_PAGE_SIZE + " 之间");
        }
    }

    /**
     * 创建默认首页请求。
     *
     * @return 页码为 1、每页 20 条的请求
     */
    public static PageRequest firstPage() {
        return new PageRequest(1, DEFAULT_PAGE_SIZE);
    }
}
