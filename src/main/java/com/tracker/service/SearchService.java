package com.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.agent.AgentFactory;
import com.tracker.agent.AppStoreSearchTool;
import com.tracker.dto.PageResult;
import com.tracker.dto.SearchTaskDTO;
import com.tracker.entity.MedicalAiAgent;
import com.tracker.entity.SearchKeywordConfig;
import com.tracker.entity.SearchTask;
import com.tracker.entity.SeedAgent;
import com.tracker.mapper.MedicalAiAgentMapper;
import com.tracker.mapper.SearchKeywordConfigMapper;
import com.tracker.mapper.SearchTaskMapper;
import com.tracker.mapper.SeedAgentMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 搜索调度服务（基于 AgentScope 智能体框架重构）
 *
 * 核心职责：
 * 1. 通过 AgentScope ReActAgent 执行完整的搜索周期
 * 2. 智能体自主决定是否调用联网搜索工具（AppStoreSearchTool + LLM自带web_search）
 * 3. 管理搜索任务的生命周期（创建 → 运行 → 完成/失败）
 * 4. 查询搜索任务历史记录
 *
 * 搜索流程（三路并发）：
 * ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
 * │  分类搜索(并发)  │  │  滚雪球搜索      │  │ App Store搜索   │
 * │  8领域×3轮      │  │  种子库+已有产品  │  │  医疗AI关键词    │
 * └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
 *          │                    │                     │
 *          └────────────────────┼─────────────────────┘
 *                               ▼
 *                     标记下线 + 汇总统计
 *
 * 并发策略：
 * - 分类搜索：8个领域并发，同一领域内按轮次顺序
 * - 滚雪球搜索：与分类搜索同时启动，使用种子库+已有产品
 * - App Store搜索：与分类搜索同时启动，直接调iTunes API
 * - 三路搜索全部完成后，执行标记下线
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final AgentFactory agentFactory;
    private final AgentService agentService;
    private final SearchTaskMapper searchTaskMapper;
    private final SearchKeywordConfigMapper keywordConfigMapper;
    private final MedicalAiAgentMapper agentMapper;
    private final SeedAgentMapper seedAgentMapper;
    private final ObjectMapper objectMapper;

    /** 并发搜索线程池（6线程：4给分类搜索 + 1给滚雪球 + 1给App Store） */
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(6);

    /**
     * 应用关闭时优雅关闭线程池
     * 防止应用停止时线程无法正常终止
     */
    @PreDestroy
    public void shutdown() {
        searchExecutor.shutdown();
        try {
            if (!searchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                searchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            searchExecutor.shutdownNow();
        }
    }

    /** App Store 搜索关键词列表 */
    private static final List<String> APP_STORE_KEYWORDS = List.of(
            "医疗AI", "AI问诊", "AI影像", "AI健康", "AI中医",
            "智能医疗", "互联网医院", "AI诊断", "医疗助手", "健康助手"
    );

    /**
     * 执行完整的搜索周期（三路并发）
     *
     * 三路搜索同时启动：
     * 1. 分类搜索：8个领域并发，每个领域内按轮次顺序执行
     * 2. 滚雪球搜索：基于种子库+已有产品，发现更多同类
     * 3. App Store搜索：直接调iTunes Search API，获取下载量/评分数据
     *
     * 全部完成后执行标记下线。
     * 注意：不加 @Transactional，因为内部使用多线程，事务无法传播到异步线程。
     *
     * @return 搜索结果统计
     */
    public Map<String, Object> executeFullSearch() {
        log.info("Starting full search cycle (3-way concurrent mode)...");
        long startTime = System.currentTimeMillis();

        // 线程安全的统计计数器
        AtomicInteger totalNew = new AtomicInteger(0);
        AtomicInteger totalUpdated = new AtomicInteger(0);
        AtomicInteger totalTasks = new AtomicInteger(0);

        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        // ═══ 第一路：分类搜索（8领域并发） ═══
        List<SearchKeywordConfig> keywords = keywordConfigMapper.selectList(
                new LambdaQueryWrapper<SearchKeywordConfig>()
                        .eq(SearchKeywordConfig::getEnabled, true)
                        .orderByAsc(SearchKeywordConfig::getCategory)
                        .orderByAsc(SearchKeywordConfig::getRound));

        Map<String, List<SearchKeywordConfig>> byCategory = keywords.stream()
                .collect(Collectors.groupingBy(SearchKeywordConfig::getCategory));

        for (Map.Entry<String, List<SearchKeywordConfig>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<SearchKeywordConfig> categoryKeywords = entry.getValue();

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (SearchKeywordConfig kwConfig : categoryKeywords) {
                    try {
                        executeSingleSearch(category, kwConfig, totalNew, totalUpdated, totalTasks);
                    } catch (Exception e) {
                        log.error("Category search failed: {} in {}", kwConfig.getKeyword(), category, e);
                    }
                }
            }, searchExecutor);
            allFutures.add(future);
        }

        // ═══ 第二路：滚雪球搜索（与分类搜索同时启动） ═══
        CompletableFuture<Void> snowballFuture = CompletableFuture.runAsync(() -> {
            try {
                executeSnowballSearch(totalNew, totalUpdated);
            } catch (Exception e) {
                log.error("Snowball search failed", e);
            }
        }, searchExecutor);
        allFutures.add(snowballFuture);

        // ═══ 第三路：App Store 搜索（与分类搜索同时启动） ═══
        CompletableFuture<Void> appStoreFuture = CompletableFuture.runAsync(() -> {
            try {
                executeAppStoreSearch(totalNew, totalUpdated, totalTasks);
            } catch (Exception e) {
                log.error("App Store search failed", e);
            }
        }, searchExecutor);
        allFutures.add(appStoreFuture);

        // 等待三路搜索全部完成
        try {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).get(30, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("Search timed out after 30 minutes");
        } catch (Exception e) {
            log.error("Search interrupted", e);
        }

        // 标记下线 - 将超过2周未被搜索验证到的智能体标记为offline
        int offlineCount = agentService.markOfflineAgents();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Full search completed in {}ms. Tasks: {}, New: {}, Updated: {}, Offline: {}",
                duration, totalTasks.get(), totalNew.get(), totalUpdated.get(), offlineCount);

        Map<String, Object> result = new HashMap<>();
        result.put("totalTasks", totalTasks.get());
        result.put("totalNew", totalNew.get());
        result.put("totalUpdated", totalUpdated.get());
        result.put("offlineCount", offlineCount);
        result.put("duration", duration);
        return result;
    }

    /**
     * 执行单个关键词的分类搜索任务
     *
     * 每个任务独立创建 ReActAgent 实例，避免多线程间的状态污染。
     *
     * @param category      细分领域
     * @param kwConfig      关键词配置
     * @param totalNew      新增计数器（线程安全）
     * @param totalUpdated  更新计数器（线程安全）
     * @param totalTasks    任务计数器（线程安全）
     */
    private void executeSingleSearch(String category, SearchKeywordConfig kwConfig,
                                     AtomicInteger totalNew, AtomicInteger totalUpdated,
                                     AtomicInteger totalTasks) {
        try {
            log.info("[Category] Searching category={}, round={}, keyword={}", category, kwConfig.getRound(), kwConfig.getKeyword());

            SearchTask task = new SearchTask();
            task.setKeyword(kwConfig.getKeyword());
            task.setCategory(category);
            task.setRound(kwConfig.getRound());
            task.setStatus("RUNNING");
            searchTaskMapper.insert(task);
            totalTasks.incrementAndGet();

            ReActAgent searchAgent = agentFactory.createSearchAgent();

            String userMessage = String.format(
                    "请搜索并整理中国国内与\"%s\"相关的AI智能体产品。搜索关键词参考：%s。请尽可能全面，不要遗漏任何已知产品。",
                    category, kwConfig.getKeyword());

            Msg inputMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .name("user")
                    .content(List.of(TextBlock.builder().text(userMessage).build()))
                    .build();

            Msg response = searchAgent.call(inputMsg).block();
            String aiResponse = response != null ? response.getTextContent() : "";

            task.setRawResponse(aiResponse);

            List<Map<String, Object>> agentList = extractAgentList(aiResponse);
            task.setResultCount(agentList.size());

            for (Map<String, Object> agentData : agentList) {
                boolean isNew = agentService.saveOrUpdateFromAI(agentData);
                if (isNew) totalNew.incrementAndGet();
                else totalUpdated.incrementAndGet();
            }

            task.setStatus("COMPLETED");
            searchTaskMapper.updateById(task);

            Thread.sleep(3000);

        } catch (Exception e) {
            log.error("[Category] Search failed: {} in {}", kwConfig.getKeyword(), category, e);
            // 更新原task状态为FAILED，而非新建task
            try {
                SearchTask runningTask = searchTaskMapper.selectOne(new LambdaQueryWrapper<SearchTask>()
                        .eq(SearchTask::getKeyword, kwConfig.getKeyword())
                        .eq(SearchTask::getStatus, "RUNNING")
                        .orderByDesc(SearchTask::getCreatedAt)
                        .last("LIMIT 1"));
                if (runningTask != null) {
                    runningTask.setStatus("FAILED");
                    runningTask.setResultCount(0);
                    searchTaskMapper.updateById(runningTask);
                }
            } catch (Exception ex) {
                log.error("Failed to update task status to FAILED", ex);
            }
        }
    }

    /**
     * 执行滚雪球搜索
     *
     * 基于种子库+已有产品列表，让AI联想出更多同类产品。
     * 种子库提供固定起点，降低搜索随机性。
     *
     * @param totalNew     新增计数器
     * @param totalUpdated 更新计数器
     */
    private void executeSnowballSearch(AtomicInteger totalNew, AtomicInteger totalUpdated) {
        log.info("[Snowball] Starting snowball search...");

        ReActAgent snowballAgent = agentFactory.createSnowballAgent();

        // 获取种子库中所有启用的种子产品（固定起点，降低随机性）
        List<SeedAgent> seeds = seedAgentMapper.selectList(
                new LambdaQueryWrapper<SeedAgent>().eq(SeedAgent::getEnabled, true));
        Set<String> seedNames = seeds.stream()
                .map(s -> s.getName() + "(" + (s.getCompany() != null ? s.getCompany() : "") + ")")
                .collect(Collectors.toSet());

        // 获取所有活跃状态的智能体
        List<MedicalAiAgent> existingAgents = agentMapper.selectList(
                new LambdaQueryWrapper<MedicalAiAgent>().eq(MedicalAiAgent::getStatus, "active"));
        Set<String> existingNames = existingAgents.stream()
                .map(a -> a.getName() + "(" + (a.getCompany() != null ? a.getCompany() : "") + ")")
                .collect(Collectors.toSet());

        // 合并种子库和已有产品（去重，种子在前）
        Set<String> allKnownNames = new LinkedHashSet<>(seedNames);
        allKnownNames.addAll(existingNames);

        if (allKnownNames.isEmpty()) {
            log.warn("[Snowball] No known products to start snowball search");
            return;
        }

        String allKnownStr = String.join("、", allKnownNames);

        // 创建搜索任务记录
        SearchTask task = new SearchTask();
        task.setKeyword("滚雪球搜索");
        task.setCategory("snowball");
        task.setRound(0);
        task.setStatus("RUNNING");
        searchTaskMapper.insert(task);

        String userMessage = String.format(
                "已知以下国内医疗AI产品：%s\n\n请找出尚未包含在上述已知列表中的其他国内医疗AI智能体产品。",
                allKnownStr);

        Msg inputMsg = Msg.builder()
                .role(MsgRole.USER)
                .name("user")
                .content(List.of(TextBlock.builder().text(userMessage).build()))
                .build();

        Msg response = snowballAgent.call(inputMsg).block();
        String aiResponse = response != null ? response.getTextContent() : "";

        task.setRawResponse(aiResponse);

        List<Map<String, Object>> snowballAgents = extractAgentList(aiResponse);
        task.setResultCount(snowballAgents.size());

        for (Map<String, Object> agentData : snowballAgents) {
            boolean isNew = agentService.saveOrUpdateFromAI(agentData);
            if (isNew) totalNew.incrementAndGet();
            else totalUpdated.incrementAndGet();
        }

        task.setStatus("COMPLETED");
        searchTaskMapper.updateById(task);

        log.info("[Snowball] Found {} products", snowballAgents.size());
    }

    /**
     * 执行 App Store 搜索
     *
     * 直接调用 iTunes Search API，搜索中国区 App Store 中的医疗AI应用。
     * 获取应用名称、开发者、评分、评分人数等数据，补充到智能体信息中。
     *
     * 搜索策略：
     * - 使用10个医疗AI相关关键词并发搜索
     * - 每个关键词搜索后，将结果与数据库中的智能体匹配
     * - 匹配成功则更新下载量、评分等字段
     * - 未匹配的App作为新产品入库
     *
     * @param totalNew     新增计数器
     * @param totalUpdated 更新计数器
     * @param totalTasks   任务计数器
     */
    private void executeAppStoreSearch(AtomicInteger totalNew, AtomicInteger totalUpdated,
                                       AtomicInteger totalTasks) {
        log.info("[AppStore] Starting App Store search with {} keywords...", APP_STORE_KEYWORDS.size());

        AppStoreSearchTool appStoreTool = new AppStoreSearchTool();

        // 创建搜索任务记录
        SearchTask task = new SearchTask();
        task.setKeyword("App Store搜索");
        task.setCategory("appstore");
        task.setRound(0);
        task.setStatus("RUNNING");
        searchTaskMapper.insert(task);
        totalTasks.incrementAndGet();

        int appCount = 0;

        for (String keyword : APP_STORE_KEYWORDS) {
            try {
                log.info("[AppStore] Searching keyword: {}", keyword);

                // 调用 iTunes Search API
                String searchResult = appStoreTool.search(keyword);

                // 将搜索结果交给AI智能体分析，提取结构化的医疗AI应用信息
                ReActAgent analyzeAgent = agentFactory.createSearchAgent();

                String userMessage = String.format(
                        "以下是App Store搜索关键词\"%s\"的结果：\n\n%s\n\n" +
                        "请从中识别出属于医疗AI智能体的应用，提取结构化信息。" +
                        "注意：需要包含appRating（评分）、downloadCount（评分人数作为下载量参考）字段。" +
                        "严格以JSON数组格式输出，不要输出其他内容。",
                        keyword, searchResult);

                Msg inputMsg = Msg.builder()
                        .role(MsgRole.USER)
                        .name("user")
                        .content(List.of(TextBlock.builder().text(userMessage).build()))
                        .build();

                Msg response = analyzeAgent.call(inputMsg).block();
                String aiResponse = response != null ? response.getTextContent() : "";

                List<Map<String, Object>> agentList = extractAgentList(aiResponse);
                appCount += agentList.size();

                for (Map<String, Object> agentData : agentList) {
                    boolean isNew = agentService.saveOrUpdateFromAI(agentData);
                    if (isNew) totalNew.incrementAndGet();
                    else totalUpdated.incrementAndGet();
                }

                // 请求间隔限流（Apple限流严格，间隔5秒避免429）
                Thread.sleep(5000);

            } catch (Exception e) {
                log.error("[AppStore] Search failed for keyword: {}", keyword, e);
            }
        }

        task.setResultCount(appCount);
        task.setStatus("COMPLETED");
        searchTaskMapper.updateById(task);

        log.info("[AppStore] Completed, found {} apps", appCount);
    }

    /**
     * 从 AI 回复文本中提取智能体 JSON 列表
     *
     * AI 回复通常包含 Markdown 格式和说明文字，JSON 数组可能被包裹在其中。
     * 本方法通过定位第一个 '[' 和最后一个 ']' 来截取 JSON 数组部分。
     *
     * @param aiResponse AI 返回的完整文本
     * @return 解析后的智能体数据列表；解析失败返回空列表
     */
    private List<Map<String, Object>> extractAgentList(String aiResponse) {
        try {
            String jsonStr = aiResponse;
            int startIdx = jsonStr.indexOf('[');
            int endIdx = jsonStr.lastIndexOf(']');
            if (startIdx >= 0 && endIdx > startIdx) {
                jsonStr = jsonStr.substring(startIdx, endIdx + 1);
            }

            JsonNode arrayNode = objectMapper.readTree(jsonStr);
            if (arrayNode.isArray()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (JsonNode item : arrayNode) {
                    Map<String, Object> map = new HashMap<>();
                    item.fields().forEachRemaining(entry -> map.put(entry.getKey(),
                            entry.getValue().isTextual() ? entry.getValue().asText() : entry.getValue().toString()));
                    result.add(map);
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to extract agent list from AI response", e);
        }
        return new ArrayList<>();
    }

    /**
     * 分页查询搜索任务历史记录
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     * @return 分页结果，包含搜索任务DTO列表和总条数，按创建时间倒序排列
     */
    public PageResult<SearchTaskDTO> getSearchTasks(int page, int size) {
        Page<SearchTask> pageParam = new Page<>(page, size);
        Page<SearchTask> result = searchTaskMapper.selectPage(pageParam,
                new LambdaQueryWrapper<SearchTask>().orderByDesc(SearchTask::getCreatedAt));
        List<SearchTaskDTO> dtoList = result.getRecords().stream().map(this::toTaskDTO).collect(Collectors.toList());
        return PageResult.of(dtoList, result.getTotal(), page, size);
    }

    /**
     * 将 SearchTask 实体转换为 SearchTaskDTO
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
