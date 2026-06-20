package com.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.config.DeepSeekConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek / 豆包 大模型 API 调用服务
 *
 * 核心职责：
 * 1. 调用火山引擎 Ark API（Responses 格式），支持可选的 web_search 联网搜索工具
 * 2. 解析 AI 返回的响应，提取纯文本内容
 * 3. 从 AI 回复中提取结构化的 JSON 智能体列表数据
 *
 * API 调用格式：火山引擎 Ark Responses API
 * - 端点：https://ark.cn-beijing.volces.com/api/v3/responses
 * - 认证：Bearer Token
 * - 请求体：包含 model、input、可选的 tools（web_search）
 * - 响应体：output 数组，包含 reasoning、web_search_call、message 等类型节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;

    /** JSON 请求体的 Content-Type */
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    /**
     * 创建 OkHttp 客户端实例
     * 超时配置：
     * - connectTimeout: 30秒，建立连接超时
     * - readTimeout: 180秒，读取响应超时（AI 生成长回复需要较长时间）
     * - writeTimeout: 30秒，写入请求超时
     *
     * @return OkHttpClient 实例
     */
    private OkHttpClient getClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 调用 AI API 进行搜索和分析
     *
     * 工作流程：
     * 1. 构建请求体，根据配置决定是否添加 web_search 工具
     * 2. 发送 POST 请求到火山引擎 Ark API
     * 3. 解析响应，提取 AI 生成的文本内容
     *
     * 两种模式：
     * - web_search 启用：AI 自动联网搜索并分析，一步到位
     * - web_search 禁用：AI 仅基于自身知识库回答（需配合外部搜索结果传入 prompt）
     *
     * @param prompt 发送给 AI 的提示词，包含搜索指令和格式要求
     * @return AI 生成的纯文本回复
     * @throws RuntimeException API 调用失败或网络异常时抛出
     */
    public String searchAndAnalyze(String prompt) {
        try {
            // 构建请求体，使用 LinkedHashMap 保证字段顺序
            java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
            bodyMap.put("model", deepSeekConfig.getModel());
            bodyMap.put("stream", false); // 不使用流式响应，等待完整结果

            // 如果配置启用了 web_search，添加联网搜索工具声明
            if (deepSeekConfig.isWebSearchEnabled()) {
                java.util.List<java.util.Map<String, Object>> tools = new java.util.ArrayList<>();
                java.util.Map<String, Object> webSearchTool = new java.util.HashMap<>();
                webSearchTool.put("type", "web_search");
                tools.add(webSearchTool);
                bodyMap.put("tools", tools);
            }

            // 构建 input 数组，采用 Responses API 的消息格式
            // 格式：[{ role: "user", content: [{ type: "input_text", text: "提示词" }] }]
            java.util.List<java.util.Map<String, Object>> input = new java.util.ArrayList<>();
            java.util.Map<String, Object> userMsg = new java.util.HashMap<>();
            userMsg.put("role", "user");
            java.util.List<java.util.Map<String, Object>> content = new java.util.ArrayList<>();
            java.util.Map<String, Object> textContent = new java.util.HashMap<>();
            textContent.put("type", "input_text");
            textContent.put("text", prompt);
            content.add(textContent);
            userMsg.put("content", content);
            input.add(userMsg);
            bodyMap.put("input", input);

            // 序列化为 JSON 字符串
            String requestBody = objectMapper.writeValueAsString(bodyMap);

            // 构建 HTTP 请求
            Request request = new Request.Builder()
                    .url(deepSeekConfig.getApiUrl())
                    .addHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_TYPE))
                    .build();

            // 执行请求并处理响应
            try (Response response = getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    log.error("DeepSeek API call failed: status={}, body={}", response.code(), errorBody);
                    throw new RuntimeException("DeepSeek API call failed: " + response.code());
                }
                String responseBody = response.body().string();
                log.debug("DeepSeek API response: {}", responseBody);
                // 从响应中提取 AI 生成的文本内容
                return extractTextFromResponse(responseBody);
            }
        } catch (IOException e) {
            log.error("DeepSeek API call error", e);
            throw new RuntimeException("DeepSeek API call error", e);
        }
    }

    /**
     * 从 AI API 响应中提取文本内容
     *
     * Responses API 的响应格式：
     * {
     *   "output": [
     *     { "type": "reasoning", ... },        // AI 推理过程（思考链）
     *     { "type": "web_search_call", ... },   // 联网搜索调用记录
     *     { "type": "message", "content": [     // 最终回复消息
     *       { "type": "output_text", "text": "实际回复内容" }
     *     ]}
     *   ]
     * }
     *
     * 本方法只提取 type=message 中 type=output_text 的文本内容
     * 同时兼容 Chat Completions API 格式（choices[0].message.content）作为降级方案
     *
     * @param responseBody API 返回的原始 JSON 字符串
     * @return AI 生成的纯文本内容
     */
    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 优先解析 Responses API 格式：从 output 数组中提取 message 内容
            JsonNode outputNode = root.get("output");
            if (outputNode != null && outputNode.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : outputNode) {
                    String type = item.has("type") ? item.get("type").asText() : "";
                    // 只处理 message 类型的节点（跳过 reasoning、web_search_call 等）
                    if ("message".equals(type)) {
                        JsonNode content = item.get("content");
                        if (content != null && content.isArray()) {
                            for (JsonNode c : content) {
                                // 提取 output_text 类型的文本内容
                                if ("output_text".equals(c.has("type") ? c.get("type").asText() : "")) {
                                    sb.append(c.has("text") ? c.get("text").asText() : "");
                                }
                            }
                        }
                    }
                }
                return sb.toString();
            }

            // 降级方案：尝试解析 Chat Completions API 格式
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }

            // 无法识别的格式，记录警告并返回原始内容
            log.warn("Unexpected response format: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
            return responseBody;
        } catch (Exception e) {
            log.error("Failed to parse DeepSeek response", e);
            return responseBody;
        }
    }

    /**
     * 从 AI 回复文本中提取智能体 JSON 列表
     *
     * AI 回复通常包含 Markdown 格式和说明文字，JSON 数组可能被包裹在其中。
     * 本方法通过定位第一个 '[' 和最后一个 ']' 来截取 JSON 数组部分。
     *
     * 示例 AI 回复：
     * "以下是国内医疗AI产品列表：\n[{"name":"推想医疗","company":"推想科技",...}]\n以上共10个产品"
     *
     * 提取后得到：
     * [{"name":"推想医疗","company":"推想科技",...}]
     *
     * @param aiResponse AI 返回的完整文本
     * @return 解析后的智能体数据列表，每个元素是一个字段名到值的映射；解析失败返回空列表
     */
    public List<java.util.Map<String, Object>> extractAgentList(String aiResponse) {
        try {
            // 在回复文本中定位 JSON 数组的起止位置
            String jsonStr = aiResponse;
            int startIdx = jsonStr.indexOf('[');
            int endIdx = jsonStr.lastIndexOf(']');
            if (startIdx >= 0 && endIdx > startIdx) {
                // 截取 JSON 数组部分
                jsonStr = jsonStr.substring(startIdx, endIdx + 1);
            }

            // 解析 JSON 数组为 Map 列表
            JsonNode arrayNode = objectMapper.readTree(jsonStr);
            if (arrayNode.isArray()) {
                List<java.util.Map<String, Object>> result = new ArrayList<>();
                for (JsonNode item : arrayNode) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    // 遍历 JSON 对象的所有字段，文本值直接取，非文本值转为字符串
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
}
