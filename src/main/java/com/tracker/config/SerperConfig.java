package com.tracker.config;

import com.tracker.service.SerperService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Serper.dev Google 搜索 API 配置类
 *
 * 对应 application.yml 中 serper.* 配置项：
 * - api-url: Serper.dev API 端点地址
 * - api-key: Serper.dev API 密钥
 *
 * 当 DeepSeek 的 web_search 工具不可用时，使用 Serper.dev 作为备选搜索方案
 * 通过 @PostConstruct 在 Bean 初始化后将 API Key 注入到 SerperService 中
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "serper")
public class SerperConfig {

    /** Serper.dev API 端点地址 */
    private String apiUrl;
    /** Serper.dev API 密钥 */
    private String apiKey;

    private final SerperService serperService;

    public SerperConfig(SerperService serperService) {
        this.serperService = serperService;
    }

    /**
     * Bean 初始化后回调，将配置中的 API Key 注入到 SerperService
     * 因为 SerperService 的 apiKey 不通过构造器注入，需要手动设置
     */
    @PostConstruct
    public void init() {
        serperService.setApiKey(apiKey);
    }
}
