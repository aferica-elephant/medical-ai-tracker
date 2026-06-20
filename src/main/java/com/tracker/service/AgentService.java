package com.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracker.dto.AgentDTO;
import com.tracker.dto.DashboardStats;
import com.tracker.dto.PageResult;
import com.tracker.entity.AgentChangeLog;
import com.tracker.entity.MedicalAiAgent;
import com.tracker.mapper.AgentChangeLogMapper;
import com.tracker.mapper.MedicalAiAgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 医疗AI智能体数据服务
 *
 * 核心职责：
 * 1. 智能体数据的 CRUD 操作（分页查询、详情查询、排行查询）
 * 2. AI 提取数据的去重入库（根据名称+公司名判断是否已存在）
 * 3. 智能体变更日志记录（新增/更新/下线）
 * 4. 仪表盘统计数据计算
 * 5. 过期智能体下线标记
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final MedicalAiAgentMapper agentMapper;
    private final AgentChangeLogMapper changeLogMapper;

    /**
     * 分页查询智能体列表，支持多条件筛选
     *
     * 筛选条件：
     * - category: 按细分领域筛选（如"AI影像诊断"）
     * - keyword: 按名称/公司/简介模糊搜索
     * - status: 按状态筛选（active/offline）
     *
     * 排序：按更新时间倒序（最近更新的排在前面）
     *
     * @param page     页码（从1开始）
     * @param size     每页条数
     * @param category 细分领域（可选）
     * @param keyword  搜索关键词（可选，模糊匹配名称/公司/简介）
     * @param status   状态筛选（可选）
     * @return 分页结果，包含记录列表和总数
     */
    public PageResult<AgentDTO> listAgents(int page, int size, String category, String keyword, String status) {
        Page<MedicalAiAgent> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MedicalAiAgent> wrapper = new LambdaQueryWrapper<>();

        // 条件筛选：细分领域
        if (StringUtils.hasText(category)) {
            wrapper.eq(MedicalAiAgent::getCategory, category);
        }
        // 条件筛选：状态
        if (StringUtils.hasText(status)) {
            wrapper.eq(MedicalAiAgent::getStatus, status);
        }
        // 条件筛选：关键词模糊搜索（名称 OR 公司 OR 简介）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MedicalAiAgent::getName, keyword)
                    .or().like(MedicalAiAgent::getCompany, keyword)
                    .or().like(MedicalAiAgent::getDescription, keyword));
        }
        // 按更新时间倒序排列
        wrapper.orderByDesc(MedicalAiAgent::getUpdatedAt);

        Page<MedicalAiAgent> result = agentMapper.selectPage(pageParam, wrapper);

        // 实体转DTO
        List<AgentDTO> dtos = result.getRecords().stream().map(this::toDTO).collect(Collectors.toList());
        return PageResult.of(dtos, result.getTotal(), page, size);
    }

    /**
     * 根据ID获取智能体详情
     *
     * @param id 智能体ID
     * @return 智能体DTO，不存在时返回null
     */
    public AgentDTO getAgent(Long id) {
        MedicalAiAgent agent = agentMapper.selectById(id);
        return agent != null ? toDTO(agent) : null;
    }

    /**
     * 获取 Top100 排行榜
     *
     * 排行规则：
     * - 仅包含 active 状态的智能体
     * - 按更新时间倒序排列（最近验证过的排在前面）
     * - 最多返回100条
     *
     * TODO: 后续可引入综合评分机制（用户量、融资额、媒体曝光等维度）
     *
     * @return Top100 智能体DTO列表
     */
    public List<AgentDTO> getRanking() {
        LambdaQueryWrapper<MedicalAiAgent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalAiAgent::getStatus, "active")
                .orderByDesc(MedicalAiAgent::getUpdatedAt)
                .last("LIMIT 100");
        return agentMapper.selectList(wrapper).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取细分领域分布统计
     * 统计各细分领域的活跃智能体数量
     *
     * @return 领域名称 → 数量的映射，如 {"AI影像诊断": 15, "AI问诊": 12, ...}
     */
    public Map<String, Integer> getCategoryDistribution() {
        List<MedicalAiAgent> agents = agentMapper.selectList(null);
        return agents.stream()
                .filter(a -> "active".equals(a.getStatus()))  // 仅统计活跃智能体
                .collect(Collectors.groupingBy(
                        a -> a.getCategory() != null ? a.getCategory() : "未分类",
                        Collectors.summingInt(a -> 1)
                ));
    }

    /**
     * 获取仪表盘统计数据
     *
     * 统计内容：
     * - totalAgents: 智能体总数
     * - activeAgents: 活跃智能体数
     * - offlineAgents: 下线智能体数
     * - categoryDistribution: 细分领域分布
     * - lastUpdateTime: 最近更新时间
     * - recentChanges: 最近变更记录
     *
     * @return 仪表盘统计数据对象
     */
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        // 统计总数、活跃数、下线数
        Long total = agentMapper.selectCount(null);
        Long active = agentMapper.selectCount(new LambdaQueryWrapper<MedicalAiAgent>()
                .eq(MedicalAiAgent::getStatus, "active"));
        Long offline = agentMapper.selectCount(new LambdaQueryWrapper<MedicalAiAgent>()
                .eq(MedicalAiAgent::getStatus, "offline"));

        stats.setTotalAgents(total.intValue());
        stats.setActiveAgents(active.intValue());
        stats.setOfflineAgents(offline.intValue());
        stats.setCategoryDistribution(getCategoryDistribution());

        // 获取最近更新时间
        MedicalAiAgent latest = agentMapper.selectOne(new LambdaQueryWrapper<MedicalAiAgent>()
                .orderByDesc(MedicalAiAgent::getUpdatedAt).last("LIMIT 1"));
        if (latest != null) {
            stats.setLastUpdateTime(latest.getUpdatedAt().toString());
        }

        // 获取最近10条变更记录
        List<AgentChangeLog> recentLogs = changeLogMapper.selectList(
                new LambdaQueryWrapper<AgentChangeLog>()
                        .orderByDesc(AgentChangeLog::getCreatedAt)
                        .last("LIMIT 10"));
        stats.setRecentChanges(new ArrayList<>());

        return stats;
    }

    /**
     * 保存或更新智能体数据（从AI提取的数据）
     *
     * 去重逻辑：根据"名称+公司名"判断是否已存在
     * - 已存在：更新字段（description、techFeatures、website、category、targetUser），记录变更日志
     * - 不存在：创建新记录，记录新增日志
     *
     * @param agentData AI 提取的智能体数据，包含 name、company、website、description、category、targetUser 等字段
     * @return true=新增，false=更新；名称为空时返回false
     */
    @Transactional
    public boolean saveOrUpdateFromAI(Map<String, Object> agentData) {
        String name = (String) agentData.getOrDefault("name", "");
        String company = (String) agentData.getOrDefault("company", "");

        // 名称必填，否则跳过
        if (!StringUtils.hasText(name)) {
            return false;
        }

        // 按"名称+公司名"查询是否已存在
        MedicalAiAgent existing = agentMapper.selectOne(new LambdaQueryWrapper<MedicalAiAgent>()
                .eq(MedicalAiAgent::getName, name)
                .eq(MedicalAiAgent::getCompany, company)
                .last("LIMIT 1"));

        if (existing != null) {
            // 已存在：更新字段并记录变更日志
            updateAgentFields(existing, agentData);
            existing.setLastVerifiedDate(LocalDate.now());  // 更新验证日期
            agentMapper.updateById(existing);
            return false;
        } else {
            // 不存在：创建新记录
            MedicalAiAgent agent = new MedicalAiAgent();
            agent.setName(name);
            agent.setCompany(company);
            agent.setWebsite((String) agentData.getOrDefault("website", ""));
            agent.setDescription((String) agentData.getOrDefault("description", ""));
            agent.setCategory((String) agentData.getOrDefault("category", ""));
            agent.setTargetUser((String) agentData.getOrDefault("targetUser", ""));
            agent.setTechFeatures((String) agentData.getOrDefault("techFeatures", ""));
            agent.setStatus("active");
            agent.setFirstFoundDate(LocalDate.now());      // 首次发现日期
            agent.setLastVerifiedDate(LocalDate.now());     // 最近验证日期
            agentMapper.insert(agent);

            // 记录新增变更日志
            AgentChangeLog changeLog = new AgentChangeLog();
            changeLog.setAgentId(agent.getId());
            changeLog.setChangeType("NEW");
            changeLog.setFieldName("ALL");
            changeLog.setNewValue(name + " - " + company);
            changeLogMapper.insert(changeLog);
            return true;
        }
    }

    /**
     * 标记过期智能体为下线状态
     *
     * 判定规则：lastVerifiedDate 超过2周的 active 智能体标记为 offline
     * 原理：如果2周内的搜索都没有再次验证到该产品，可能已下线或停止运营
     *
     * @return 被标记为下线的智能体数量
     */
    @Transactional
    public int markOfflineAgents() {
        // 计算2周前的日期作为截止线
        LocalDate cutoffDate = LocalDate.now().minusWeeks(2);
        // 查询所有超过2周未验证的活跃智能体
        List<MedicalAiAgent> agents = agentMapper.selectList(new LambdaQueryWrapper<MedicalAiAgent>()
                .eq(MedicalAiAgent::getStatus, "active")
                .lt(MedicalAiAgent::getLastVerifiedDate, cutoffDate));

        int count = 0;
        for (MedicalAiAgent agent : agents) {
            // 更新状态为下线
            agent.setStatus("offline");
            agentMapper.updateById(agent);

            // 记录下线变更日志
            AgentChangeLog changeLog = new AgentChangeLog();
            changeLog.setAgentId(agent.getId());
            changeLog.setChangeType("OFFLINE");
            changeLog.setFieldName("status");
            changeLog.setOldValue("active");
            changeLog.setNewValue("offline");
            changeLogMapper.insert(changeLog);
            count++;
        }
        return count;
    }

    /**
     * 更新智能体字段，并记录变更日志
     *
     * 仅当新值非空且与旧值不同时才更新，避免覆盖有效数据
     * 对 description 字段额外记录变更日志（其他字段仅更新不记录日志，减少日志量）
     *
     * @param agent 待更新的智能体实体
     * @param data  AI 提取的新数据
     */
    private void updateAgentFields(MedicalAiAgent agent, Map<String, Object> data) {
        String newDesc = (String) data.getOrDefault("description", "");
        String newFeatures = (String) data.getOrDefault("techFeatures", "");
        String newWebsite = (String) data.getOrDefault("website", "");
        String newCategory = (String) data.getOrDefault("category", "");
        String newTargetUser = (String) data.getOrDefault("targetUser", "");

        // 更新简介，并记录变更日志
        if (StringUtils.hasText(newDesc) && !newDesc.equals(agent.getDescription())) {
            AgentChangeLog log = new AgentChangeLog();
            log.setAgentId(agent.getId());
            log.setChangeType("UPDATED");
            log.setFieldName("description");
            log.setOldValue(agent.getDescription());
            log.setNewValue(newDesc);
            changeLogMapper.insert(log);
            agent.setDescription(newDesc);
        }
        // 更新技术特征
        if (StringUtils.hasText(newFeatures) && !newFeatures.equals(agent.getTechFeatures())) {
            agent.setTechFeatures(newFeatures);
        }
        // 更新官网
        if (StringUtils.hasText(newWebsite) && !newWebsite.equals(agent.getWebsite())) {
            agent.setWebsite(newWebsite);
        }
        // 更新细分领域
        if (StringUtils.hasText(newCategory) && !newCategory.equals(agent.getCategory())) {
            agent.setCategory(newCategory);
        }
        // 更新面向用户
        if (StringUtils.hasText(newTargetUser) && !newTargetUser.equals(agent.getTargetUser())) {
            agent.setTargetUser(newTargetUser);
        }
    }

    /**
     * 将 MedicalAiAgent 实体转换为 AgentDTO
     * 主要处理日期格式转换（LocalDate → String），方便前端展示
     *
     * @param agent 智能体实体
     * @return 智能体DTO
     */
    private AgentDTO toDTO(MedicalAiAgent agent) {
        AgentDTO dto = new AgentDTO();
        BeanUtils.copyProperties(agent, dto);
        if (agent.getFirstFoundDate() != null) {
            dto.setFirstFoundDate(agent.getFirstFoundDate().toString());
        }
        if (agent.getLastVerifiedDate() != null) {
            dto.setLastVerifiedDate(agent.getLastVerifiedDate().toString());
        }
        return dto;
    }
}
