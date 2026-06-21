package com.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 医疗AI智能体追踪系统 - 启动类
 *
 * 功能说明：
 * - @SpringBootApplication: Spring Boot 自动配置入口
 * - @EnableScheduling: 启用定时任务调度，支持每周自动搜索更新
 *
 * 系统核心流程：
 * 1. 定时/手动触发搜索任务
 * 2. 通过 AgentScope ReActAgent 智能体联网搜索医疗AI产品信息
 * 3. 智能体自主推理和调用工具，提取结构化产品数据
 * 4. 去重入库，生成 Top100 排行
 */
@SpringBootApplication
@EnableScheduling
public class MedicalAiTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalAiTrackerApplication.class, args);
    }

}
