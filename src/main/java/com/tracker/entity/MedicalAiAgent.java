package com.tracker.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医疗AI智能体实体类
 *
 * 对应数据库表：medical_ai_agent
 * 存储所有被追踪的医疗AI智能体产品信息
 *
 * 核心字段说明：
 * - name + company: 联合唯一标识，用于去重判断
 * - status: 智能体当前状态（active=活跃, offline=下线）
 * - firstFoundDate: 首次被系统发现的时间
 * - lastVerifiedDate: 最近一次搜索验证到的时间，超过2周未验证则标记为offline
 */
@Data
@TableName("medical_ai_agent")
public class MedicalAiAgent {
    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产品名称（如"推想医疗"、"鹰瞳科技"） */
    private String name;

    /** 所属公司名称 */
    private String company;

    /** 官网URL */
    private String website;

    /** 产品简介描述 */
    private String description;

    /** 细分领域（如"AI影像诊断"、"AI问诊"、"AI药物研发"等） */
    private String category;

    /** 面向用户群体（如"医生"、"患者"、"研发人员"） */
    private String targetUser;

    /** 技术特征描述 */
    private String techFeatures;

    /** 状态：active=活跃, offline=下线 */
    private String status;

    /** 首次发现日期 */
    private LocalDate firstFoundDate;

    /** 最近验证日期（每次搜索命中时更新） */
    private LocalDate lastVerifiedDate;

    /** 创建时间，自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
