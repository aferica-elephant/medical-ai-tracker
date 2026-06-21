package com.tracker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tracker.dto.PageResult;
import com.tracker.entity.AgentChangeLog;
import com.tracker.mapper.AgentChangeLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 变更日志控制器
 *
 * 提供1个API端点：
 * - GET /api/changes  分页查询智能体变更日志
 *
 * 变更类型包括：
 * - NEW: 新增智能体
 * - UPDATED: 智能体信息更新
 * - OFFLINE: 智能体被标记为下线
 */
@RestController
@RequestMapping("/api/changes")
@RequiredArgsConstructor
public class ChangeLogController {

    private final AgentChangeLogMapper changeLogMapper;

    /**
     * 分页查询变更日志
     * 按创建时间倒序排列（最新的变更排在前面）
     *
     * @param page 页码，默认1
     * @param size 每页条数，默认20
     * @return 分页结果，包含记录列表和总条数
     */
    @GetMapping
    public PageResult<AgentChangeLog> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AgentChangeLog> pageParam = new Page<>(page, size);
        Page<AgentChangeLog> result = changeLogMapper.selectPage(pageParam,
                new LambdaQueryWrapper<AgentChangeLog>()
                        .orderByDesc(AgentChangeLog::getCreatedAt));
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }
}
