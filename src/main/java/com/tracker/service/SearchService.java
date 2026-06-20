package com.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracker.config.DeepSeekConfig;
import com.tracker.dto.SearchTaskDTO;
import com.tracker.entity.MedicalAiAgent;
import com.tracker.entity.SearchKeywordConfig;
import com.tracker.entity.SearchTask;
import com.tracker.mapper.MedicalAiAgentMapper;
import com.tracker.mapper.SearchKeywordConfigMapper;
import com.tracker.mapper.SearchTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索调度服务
 *
 * 核心职责：
 * 1. 执行完整的搜索周期（分类搜索 → 滚雪球扩展 → 标记下线）
 * 2. 根据配置选择搜索模式（AI联网搜索 / Serper.dev外部搜索 / 纯知识库）
 * 3. 管理搜索任务的生命周期（创建 → 运行 → 完成/失败）
 * 4. 查询搜索任务历史记录
 *
 * 搜索流程：
 * Step 1: 分类搜索 - 按8个细分领域（影像/问诊/药物/手术/健康/中医/病历/检验）逐个搜索
 * Step 2: 滚雪球搜索 - 基于已发现的产品，让AI寻找更多同类产品
 * Step 3: 标记下线 - 将超过2周未验证的智能体标记为offline
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final DeepSeekService deepSeekService;
    private final SerperService serperService;
    private final AgentService agentService;
    private final SearchTaskMapper searchTaskMapper;
    private final SearchKeywordConfigMapper keywordConfigMapper;
    private final MedicalAiAgentMapper agentMapper;
    private final DeepSeekConfig deepSeekConfig;

    /**
     * 执行完整的搜索周期
     *
     * 整体流程：
     * 1. 从数据库读取所有启用的搜索关键词配置
     * 2. 按细分领域分组，逐个关键词执行搜索
     * 3. 根据配置选择搜索模式：
     *    - web_search 启用：AI 直接联网搜索+分析（推荐，一步到位）
     *    - web_search 禁用：先 Serper.dev 搜索，再 AI 分析搜索结果
     * 4. AI 返回 JSON 格式的智能体列表，解析后逐个入库
     * 5. 执行滚雪球搜索，发现更多产品
     * 6. 标记超过2周未验证的智能体为下线状态
     *
     * @return 搜索结果统计，包含 totalTasks、totalNew、totalUpdated、offlineCount、duration
     */
    @Transactional
    public Map<String, Object> executeFullSearch() {
        // 判断搜索模式
        boolean useWebSearch = deepSeekConfig.isWebSearchEnabled();
        log.info("Starting full search cycle (web_search={})...", useWebSearch);
        long startTime = System.currentTimeMillis();

        // 统计计数器
        int totalNew = 0;      // 新增智能体数量
        int totalUpdated = 0;  // 更新智能体数量
        int totalTasks = 0;    // 执行的搜索任务数量

        // Step 1: 从数据库读取所有启用的搜索关键词配置
        List<SearchKeywordConfig> keywords = keywordConfigMapper.selectList(
                new LambdaQueryWrapper<SearchKeywordConfig>()
                        .eq(SearchKeywordConfig::getEnabled, true)
                        .orderByAsc(SearchKeywordConfig::getCategory)
                        .orderByAsc(SearchKeywordConfig::getRound));

        // 按细分领域分组，同一领域的关键词按轮次顺序执行
        Map<String, List<SearchKeywordConfig>> byCategory = keywords.stream()
                .collect(Collectors.groupingBy(SearchKeywordConfig::getCategory));

        // 遍历每个细分领域，逐个关键词执行搜索
        for (Map.Entry<String, List<SearchKeywordConfig>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<SearchKeywordConfig> categoryKeywords = entry.getValue();

            for (SearchKeywordConfig kwConfig : categoryKeywords) {
                try {
                    log.info("Searching category={}, round={}, keyword={}", category, kwConfig.getRound(), kwConfig.getKeyword());

                    // 创建搜索任务记录，状态为 RUNNING
                    SearchTask task = new SearchTask();
                    task.setKeyword(kwConfig.getKeyword());
                    task.setCategory(category);
                    task.setRound(kwConfig.getRound());
                    task.setStatus("RUNNING");
                    searchTaskMapper.insert(task);
                    totalTasks++;

                    // 根据搜索模式选择不同的 prompt 模板
                    String prompt;
                    if (useWebSearch) {
                        // 模式1：AI自带联网搜索，直接用搜索prompt，AI会自动搜索+分析
                        prompt = PromptTemplates.searchByCategoryWithWeb(category, kwConfig.getKeyword());
                    } else {
                        // 模式2：先用Serper搜索获取结果，再让AI分析
                        String searchResults = serperService.searchAndFormat(kwConfig.getKeyword());
                        if (StringUtils.hasText(searchResults)) {
                            // 有搜索结果：将结果作为上下文传给AI
                            prompt = PromptTemplates.searchWithResults(category, kwConfig.getKeyword(), searchResults);
                        } else {
                            // 无搜索结果：降级为纯知识库模式
                            prompt = PromptTemplates.searchByCategory(category, kwConfig.getKeyword());
                        }
                    }

                    // 调用AI进行分析
                    String aiResponse = deepSeekService.searchAndAnalyze(prompt);

                    // 保存AI原始回复（用于调试和审计）
                    task.setRawResponse(aiResponse);

                    // 从AI回复中提取结构化的智能体列表
                    List<Map<String, Object>> agentList = deepSeekService.extractAgentList(aiResponse);
                    task.setResultCount(agentList.size());

                    // 逐个保存或更新智能体数据
                    for (Map<String, Object> agentData : agentList) {
                        boolean isNew = agentService.saveOrUpdateFromAI(agentData);
                        if (isNew) totalNew++;
                        else totalUpdated++;
                    }

                    // 更新任务状态为完成
                    task.setStatus("COMPLETED");
                    searchTaskMapper.updateById(task);

                    // 请求间隔限流，避免API调用过于频繁
                    // web_search 模式间隔更长（5秒），因为AI需要联网搜索
                    Thread.sleep(useWebSearch ? 5000 : 3000);

                } catch (Exception e) {
                    log.error("Search failed for keyword: {}", kwConfig.getKeyword(), e);
                    // 搜索失败时记录失败任务
                    SearchTask failTask = new SearchTask();
                    failTask.setKeyword(kwConfig.getKeyword());
                    failTask.setCategory(category);
                    failTask.setRound(kwConfig.getRound());
                    failTask.setStatus("FAILED");
                    failTask.setResultCount(0);
                    searchTaskMapper.insert(failTask);
                }
            }
        }

        // Step 2: 滚雪球搜索 - 基于已发现的产品，寻找更多同类产品
        try {
            log.info("Starting snowball search...");
            // 获取所有活跃状态的智能体
            List<MedicalAiAgent> existingAgents = agentMapper.selectList(
                    new LambdaQueryWrapper<MedicalAiAgent>().eq(MedicalAiAgent::getStatus, "active"));
            // 拼接已有产品名称列表，格式："产品名(公司名)、产品名(公司名)"
            String existingNames = existingAgents.stream()
                    .map(a -> a.getName() + "(" + a.getCompany() + ")")
                    .collect(Collectors.joining("、"));

            if (StringUtils.hasText(existingNames)) {
                String prompt;
                if (useWebSearch) {
                    // AI联网搜索模式：让AI联网搜索更多产品
                    prompt = PromptTemplates.snowballSearchWithWeb(existingNames);
                } else {
                    // 外部搜索模式：先Serper搜索，再AI分析
                    String searchResults = serperService.searchAndFormat("国内医疗AI智能体 最新产品 2024 2025");
                    if (StringUtils.hasText(searchResults)) {
                        prompt = PromptTemplates.snowballSearchWithResults(existingNames, searchResults);
                    } else {
                        prompt = PromptTemplates.snowballSearch(existingNames);
                    }
                }
                String aiResponse = deepSeekService.searchAndAnalyze(prompt);

                // 解析并入库滚雪球搜索发现的新产品
                List<Map<String, Object>> snowballAgents = deepSeekService.extractAgentList(aiResponse);
                for (Map<String, Object> agentData : snowballAgents) {
                    boolean isNew = agentService.saveOrUpdateFromAI(agentData);
                    if (isNew) totalNew++;
                    else totalUpdated++;
                }
            }
        } catch (Exception e) {
            log.error("Snowball search failed", e);
        }

        // Step 3: 标记下线 - 将超过2周未被搜索验证到的智能体标记为offline
        int offlineCount = agentService.markOfflineAgents();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Full search completed in {}ms. New: {}, Updated: {}, Offline: {}", duration, totalNew, totalUpdated, offlineCount);

        // 返回搜索结果统计
        Map<String, Object> result = new HashMap<>();
        result.put("totalTasks", totalTasks);
        result.put("totalNew", totalNew);
        result.put("totalUpdated", totalUpdated);
        result.put("offlineCount", offlineCount);
        result.put("duration", duration);
        return result;
    }

    /**
     * 分页查询搜索任务历史记录
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 搜索任务DTO列表，按创建时间倒序排列
     */
    public List<SearchTaskDTO> getSearchTasks(int page, int size) {
        Page<SearchTask> pageParam = new Page<>(page, size);
        Page<SearchTask> result = searchTaskMapper.selectPage(pageParam,
                new LambdaQueryWrapper<SearchTask>().orderByDesc(SearchTask::getCreatedAt));
        return result.getRecords().stream().map(this::toTaskDTO).collect(Collectors.toList());
    }

    /**
     * 将 SearchTask 实体转换为 SearchTaskDTO
     * 主要处理日期格式转换（LocalDateTime → String）
     *
     * @param task 搜索任务实体
     * @return 搜索任务DTO
     */
    private SearchTaskDTO toTaskDTO(SearchTask task) {
        SearchTaskDTO dto = new SearchTaskDTO();
        BeanUtils.copyProperties(task, dto);
        if (task.getCreatedAt() != null) {
            dto.setCreatedAt(task.getCreatedAt().toString());
        }
        return dto;
    }
}
