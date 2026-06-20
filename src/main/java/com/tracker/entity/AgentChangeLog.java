package com.tracker.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能体变更日志实体类
 *
 * 对应数据库表：agent_change_log
 * 记录智能体数据的所有变更，用于追踪和审计
 *
 * 变更类型：
 * - NEW: 新增智能体
 * - UPDATED: 智能体信息更新（如简介变更）
 * - OFFLINE: 智能体被标记为下线
 */
@Data
@TableName("agent_change_log")
public class AgentChangeLog {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的智能体ID */
    private Long agentId;

    /** 变更类型：NEW=新增, UPDATED=更新, OFFLINE=下线 */
    private String changeType;

    /** 变更字段名称（如"description"、"status"、"ALL"表示整条新增） */
    private String fieldName;

    /** 变更前的值 */
    private String oldValue;

    /** 变更后的值 */
    private String newValue;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
