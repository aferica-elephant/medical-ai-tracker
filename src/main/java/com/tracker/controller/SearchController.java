package com.tracker.controller;

import com.tracker.dto.PageResult;
import com.tracker.dto.SearchTaskDTO;
import com.tracker.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索管理控制器
 *
 * 提供2个API端点：
 * - POST   /api/search/trigger  手动触发全量搜索
 * - GET    /api/search/tasks    查询搜索任务历史
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 手动触发全量搜索
     * 执行完整的搜索周期：分类搜索 → 滚雪球扩展 → 标记下线
     *
     * 注意：此操作耗时较长（5-10分钟），因为需要逐个调用AI API
     *
     * @return 搜索结果统计，包含 totalTasks、totalNew、totalUpdated、offlineCount、duration
     */
    @PostMapping("/trigger")
    public Map<String, Object> triggerSearch() {
        return searchService.executeFullSearch();
    }

    /**
     * 分页查询搜索任务历史记录
     *
     * @param page 页码，默认1
     * @param size 每页条数，默认20
     * @return 分页结果，包含搜索任务DTO列表和总条数
     */
    @GetMapping("/tasks")
    public PageResult<SearchTaskDTO> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return searchService.getSearchTasks(page, size);
    }
}
