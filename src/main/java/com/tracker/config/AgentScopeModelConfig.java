package com.tracker.config;

import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 模型配置类
 *
 * 手动创建 OpenAIChatModel Bean，使用 OpenAI 兼容格式对接火山引擎 Ark API。
 *
 * 火山引擎 Ark API 完全兼容 OpenAI Chat Completions 格式：
 * - 端点：POST {base-url}/chat/completions
 * - 认证：Authorization: Bearer {api-key}
 * - 请求体：{"model": "...", "messages": [...], "stream": false}
 *
 * 配置项读取自 application.yml 中的 agentscope.openai.* 前缀，
 * 与 Starter 标准配置格式一致。
 */
@Slf4j
@Configuration
public class AgentScopeModelConfig {

    /**
     * 创建 OpenAI 兼容格式的 Model Bean
     *
     * 使用 agentscope-core 自带的 OpenAIChatModel，
     * 通过 OpenAI 兼容格式对接火山引擎 Ark API。
     *
     * @param baseUrl   API 基础 URL（如 https://ark.cn-beijing.volces.com/api/v3）
     * @param apiKey    API 密钥
     * @param modelName 模型名称（如 deepseek-v4-flash-260425）
     * @param stream    是否使用流式响应
     * @return OpenAIChatModel 实例
     */
    @Bean
    public Model agentscopeModel(
            @Value("${agentscope.openai.base-url:https://ark.cn-beijing.volces.com/api/v3}") String baseUrl,
            @Value("${agentscope.openai.api-key:}") String apiKey,
            @Value("${agentscope.openai.model-name:deepseek-v4-flash-260425}") String modelName,
            @Value("${agentscope.openai.stream:false}") boolean stream) {
        log.info("Creating OpenAIChatModel: baseUrl={}, model={}, stream={}", baseUrl, modelName, stream);
        return OpenAIChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(stream)
                .build();
    }
}
