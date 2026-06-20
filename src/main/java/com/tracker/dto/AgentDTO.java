package com.tracker.dto;

import lombok.Data;

/**
 * 智能体数据传输对象
 *
 * 用于将 MedicalAiAgent 实体的数据传递给前端
 * 与实体的区别：日期字段从 LocalDate 转为 String，方便前端直接展示
 */
@Data
public class AgentDTO {
    /** 智能体ID */
    private Long id;
    /** 产品名称 */
    private String name;
    /** 所属公司 */
    private String company;
    /** 官网URL */
    private String website;
    /** 产品简介 */
    private String description;
    /** 细分领域 */
    private String category;
    /** 面向用户 */
    private String targetUser;
    /** 技术特征 */
    private String techFeatures;
    /** 状态（active/offline） */
    private String status;
    /** 首次发现日期（字符串格式） */
    private String firstFoundDate;
    /** 最近验证日期（字符串格式） */
    private String lastVerifiedDate;
}
