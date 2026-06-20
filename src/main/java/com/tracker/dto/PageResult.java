package com.tracker.dto;

import lombok.Data;
import java.util.List;

/**
 * 通用分页结果封装
 *
 * 将分页查询的结果统一封装，包含数据列表和分页元信息
 * 前端根据 total、page、size 计算总页数和分页控件
 *
 * @param <T> 数据项类型
 */
@Data
public class PageResult<T> {
    /** 当前页数据列表 */
    private List<T> records;
    /** 总记录数 */
    private Long total;
    /** 当前页码（从1开始） */
    private Integer page;
    /** 每页条数 */
    private Integer size;

    /**
     * 静态工厂方法，快速构建分页结果
     *
     * @param records 当前页数据列表
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页条数
     * @param <T>     数据项类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }
}
