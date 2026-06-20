package com.tracker.scheduler;

import com.tracker.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 搜索定时调度器
 *
 * 功能：每周一凌晨2:00自动执行全量搜索
 * 调度表达式通过 application.yml 中的 search.cron 配置
 * 默认值："0 0 2 ? * MON"（每周一凌晨2点）
 *
 * 注意：首次运行需要手动触发搜索（通过前端或API），定时任务仅负责后续的周期性更新
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchScheduler {

    private final SearchService searchService;

    /**
     * 定时执行全量搜索
     * 由 Spring @Scheduled 注解驱动，cron 表达式从配置文件读取
     * 异常不会中断调度，仅记录错误日志
     */
    @Scheduled(cron = "${search.cron}")
    public void weeklySearch() {
        log.info("Weekly search triggered by scheduler");
        try {
            searchService.executeFullSearch();
        } catch (Exception e) {
            log.error("Weekly search failed", e);
        }
    }
}
