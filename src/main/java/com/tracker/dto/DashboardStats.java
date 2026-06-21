package com.tracker.dto;

import lombok.Data;
import java.util.Map;
import java.util.List;

/**
 * 仪表盘统计数据传输对象
 *
 * 聚合展示系统核心指标，供前端仪表盘页面使用
 */
@Data
public class DashboardStats {
    /** 智能体总数 */
    private Integer totalAgents;
    /** 活跃智能体数 */
    private Integer activeAgents;
    /** 下线智能体数 */
    private Integer offlineAgents;
    /** 细分领域分布（领域名称 → 数量） */
    private Map<String, Integer> categoryDistribution;
    /** 最近更新时间 */
    private String lastUpdateTime;
    /** 最近变更记录列表（每条包含 agentId, changeType, fieldName, newValue, createdAt） */
    private List<Map<String, Object>> recentChanges;
}
