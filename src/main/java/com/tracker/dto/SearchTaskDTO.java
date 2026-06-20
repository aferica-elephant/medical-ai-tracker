package com.tracker.dto;

import lombok.Data;

/**
 * 搜索任务数据传输对象
 *
 * 用于将 SearchTask 实体的数据传递给前端
 * 与实体的区别：createdAt 从 LocalDateTime 转为 String，方便前端直接展示
 */
@Data
public class SearchTaskDTO {
    /** 任务ID */
    private Long id;
    /** 搜索关键词 */
    private String keyword;
    /** 所属细分领域 */
    private String category;
    /** 搜索轮次 */
    private Integer round;
    /** 发现的智能体数量 */
    private Integer resultCount;
    /** 任务状态（PENDING/RUNNING/COMPLETED/FAILED） */
    private String status;
    /** 创建时间（字符串格式） */
    private String createdAt;
}
