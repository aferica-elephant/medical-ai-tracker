package com.tracker.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 种子智能体实体类
 *
 * 对应数据库表：seed_agent
 * 存储滚雪球搜索的固定种子产品，降低搜索随机性
 *
 * 设计思路：
 * 每次滚雪球搜索时，系统会将种子库中的产品与数据库中已发现的产品合并，
 * 作为"已知产品列表"交给AI，让AI基于这些种子联想出更多同类产品。
 * 种子库中的产品是主流、知名的医疗AI产品，确保每次滚雪球都有稳定的起点。
 */
@Data
@TableName("seed_agent")
public class SeedAgent {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产品名称（如"蚂蚁阿福"） */
    private String name;

    /** 所属公司（如"蚂蚁集团"） */
    private String company;

    /** 细分领域（如"AI问诊"） */
    private String category;

    /** 是否启用该种子 */
    private Boolean enabled;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
