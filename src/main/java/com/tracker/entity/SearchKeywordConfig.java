package com.tracker.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 搜索关键词配置实体类
 *
 * 对应数据库表：search_keyword_config
 * 存储按细分领域分类的搜索关键词配置
 *
 * 搜索策略：8个细分领域 × 3轮搜索
 * 每个领域配置多个关键词，按轮次顺序执行
 * enabled字段控制是否启用该关键词
 */
@Data
@TableName("search_keyword_config")
public class SearchKeywordConfig {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 细分领域（如"AI影像诊断"、"AI问诊"等） */
    private String category;

    /** 搜索关键词 */
    private String keyword;

    /** 搜索轮次（1=广搜, 2=深挖, 3=滚雪球） */
    private Integer round;

    /** 是否启用该关键词 */
    private Boolean enabled;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
