package com.tracker.controller;

import com.tracker.dto.AgentDTO;
import com.tracker.dto.DashboardStats;
import com.tracker.dto.PageResult;
import com.tracker.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 智能体管理控制器
 *
 * 提供4个API端点：
 * - GET    /api/agents          分页查询智能体列表（支持筛选）
 * - GET    /api/agents/{id}     获取智能体详情
 * - GET    /api/agents/rank     获取Top100排行榜
 * - GET    /api/agents/categories 获取细分领域分布统计
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * 分页查询智能体列表
     *
     * @param page     页码，默认1
     * @param size     每页条数，默认20
     * @param category 细分领域筛选（可选）
     * @param keyword  关键词搜索（可选，模糊匹配名称/公司/简介）
     * @param status   状态筛选（可选，active/offline）
     * @return 分页结果
     */
    @GetMapping
    public PageResult<AgentDTO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return agentService.listAgents(page, size, category, keyword, status);
    }

    /**
     * 获取智能体详情
     *
     * @param id 智能体ID
     * @return 智能体详情DTO
     */
    @GetMapping("/{id}")
    public AgentDTO get(@PathVariable Long id) {
        return agentService.getAgent(id);
    }

    /**
     * 获取Top100排行榜
     * 返回活跃智能体按更新时间倒序排列的前100名
     *
     * @return Top100智能体列表
     */
    @GetMapping("/rank")
    public List<AgentDTO> rank() {
        return agentService.getRanking();
    }

    /**
     * 获取细分领域分布统计
     * 返回各细分领域的活跃智能体数量
     *
     * @return 领域名称 → 数量的映射
     */
    @GetMapping("/categories")
    public Map<String, Integer> categories() {
        return agentService.getCategoryDistribution();
    }
}
