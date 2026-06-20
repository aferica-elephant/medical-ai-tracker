package com.tracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek / 豆包 大模型 API 配置类
 *
 * 对应 application.yml 中 deepseek.* 配置项：
 * - api-url: 火山引擎 Ark API 端点地址
 * - api-key: API 认证密钥
 * - model: 使用的模型名称（如 doubao-seed-1-6-250615）
 * - web-search-enabled: 是否启用 AI 自带的联网搜索工具
 *   - true: AI 自动联网搜索+分析，一步到位（推荐）
 *   - false: 需配合 Serper.dev 先搜索再分析
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {
    /** API 端点地址 */
    private String apiUrl;
    /** API 认证密钥 */
    private String apiKey;
    /** 模型名称 */
    private String model;
    /** 是否启用 AI 自带的 web_search 联网搜索工具，默认关闭 */
    private boolean webSearchEnabled = false;
}
