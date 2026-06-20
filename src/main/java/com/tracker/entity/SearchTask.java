package com.tracker.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 搜索任务实体类
 *
 * 对应数据库表：search_task
 * 记录每次搜索任务的执行情况和结果
 *
 * 状态流转：PENDING → RUNNING → COMPLETED / FAILED
 */
@Data
@TableName("search_task")
public class SearchTask {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索关键词 */
    private String keyword;

    /** 所属细分领域 */
    private String category;

    /** 搜索轮次（同一领域可能有多个轮次的关键词） */
    private Integer round;

    /** 本次搜索发现的智能体数量 */
    private Integer resultCount;

    /** 任务状态：PENDING=待执行, RUNNING=执行中, COMPLETED=已完成, FAILED=失败 */
    private String status;

    /** AI原始回复内容（用于调试和审计） */
    private String rawResponse;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
