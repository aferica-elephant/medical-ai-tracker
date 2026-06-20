package com.tracker.controller;

import com.tracker.dto.DashboardStats;
import com.tracker.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘控制器
 *
 * 提供1个API端点：
 * - GET /api/dashboard/stats  获取仪表盘统计数据
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AgentService agentService;

    /**
     * 获取仪表盘统计数据
     * 包含：智能体总数、活跃数、下线数、领域分布、最近更新时间
     *
     * @return 仪表盘统计数据对象
     */
    @GetMapping("/stats")
    public DashboardStats stats() {
        return agentService.getDashboardStats();
    }
}
