package com.hengxue.common.core.api;

import java.util.List;
import java.util.Objects;

/** 通用分页响应数据载荷：{@code {items, page, pageSize, total}}。 */
public record PageResponse<T>(
        /** 当前页数据的不可变快照。 */
        List<T> items,
        /** 从 1 开始的当前页码。 */
        int page,
        /** 当前页的每页数量。 */
        int pageSize,
        /** 满足查询条件的记录总数。 */
        long total
) {

    /**
     * 创建分页响应并校验通用分页约束。
     *
     * @param items 当前页数据，会复制为不可变列表
     * @param page 从 1 开始的当前页码
     * @param pageSize 当前页的每页数量，最大为 100
     * @param total 满足查询条件的记录总数，不能为负数
     */
    public PageResponse {
        // 防止调用方后续修改源集合，导致已构造的响应内容变化。
        items = List.copyOf(Objects.requireNonNull(items, "当前页数据不能为空"));
        new PageRequest(page, pageSize);
        if (total < 0) {
            throw new IllegalArgumentException("记录总数不能为负数");
        }
    }

    /**
     * 根据分页请求创建分页响应。
     *
     * @param items 当前页数据
     * @param request 已校验的分页请求
     * @param total 满足查询条件的记录总数
     * @param <T> 列表元素类型
     * @return 复制后的不可变分页响应
     */
    public static <T> PageResponse<T> of(List<T> items, PageRequest request, long total) {
        Objects.requireNonNull(request, "分页请求不能为空");
        return new PageResponse<>(items, request.page(), request.pageSize(), total);
    }
}
