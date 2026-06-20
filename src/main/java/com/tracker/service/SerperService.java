package com.tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Serper.dev Google 搜索 API 服务
 *
 * 核心职责：
 * 1. 调用 Serper.dev API 执行 Google 搜索，获取实时搜索结果
 * 2. 解析搜索结果，提取标题、链接、摘要等结构化信息
 * 3. 将搜索结果格式化为文本，供 DeepSeek AI 分析使用
 *
 * 使用场景：
 * 当 AI 模型不支持 web_search 联网搜索工具时，使用本服务作为外部搜索方案。
 * 搜索流程：Serper.dev 搜索 → 获取结果文本 → 喂给 DeepSeek 分析提取
 *
 * Serper.dev API 文档：https://serper.dev/
 * 请求格式：POST JSON，Header 中携带 X-API-KEY
 * 响应格式：包含 organic（自然搜索结果）和 knowledgeGraph（知识图谱）等字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerperService {

    private final ObjectMapper objectMapper;

    /** Serper.dev API 端点 */
    private static final String SERPER_API_URL = "https://google.serper.dev/search";
    /** JSON 请求体的 Content-Type */
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    /** API Key，通过 SerperConfig 在启动时注入 */
    private String apiKey;

    /**
     * 设置 API Key（由 SerperConfig 调用）
     *
     * @param apiKey Serper.dev API 密钥
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 创建 OkHttp 客户端实例
     * 超时配置较保守，因为搜索 API 响应通常较快
     *
     * @return OkHttpClient 实例
     */
    private OkHttpClient getClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行搜索并返回结构化结果
     *
     * 请求参数说明：
     * - q: 搜索关键词
     * - gl: 地理位置限定为中国（cn）
     * - hl: 语言限定为中文（zh-cn）
     * - num: 返回结果数量，默认10条
     *
     * @param query 搜索关键词
     * @return 搜索结果列表，每条包含 title、link、snippet 三个字段；搜索失败返回空列表
     */
    public List<Map<String, String>> search(String query) {
        try {
            // 构建请求体
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("q", query);
            bodyMap.put("gl", "cn");      // 地理位置限定：中国
            bodyMap.put("hl", "zh-cn");   // 语言限定：简体中文
            bodyMap.put("num", 10);       // 返回结果数量

            String requestBody = objectMapper.writeValueAsString(bodyMap);

            // 构建 HTTP 请求，使用 X-API-KEY Header 认证
            Request request = new Request.Builder()
                    .url(SERPER_API_URL)
                    .addHeader("X-API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON_TYPE))
                    .build();

            // 执行请求
            try (Response response = getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    log.error("Serper API call failed: status={}, body={}", response.code(), errorBody);
                    throw new RuntimeException("Serper API call failed: " + response.code());
                }
                String responseBody = response.body().string();
                return parseSearchResults(responseBody);
            }
        } catch (Exception e) {
            log.error("Serper API search error for query: {}", query, e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索并将结果格式化为文本，供 DeepSeek 分析
     *
     * 输出格式示例：
     * 搜索关键词：国内AI影像诊断产品
     *
     * 搜索结果：
     * 1. 标题：推想医疗 - AI影像诊断
     *    链接：https://www.infervision.com
     *    摘要：推想医疗是国内领先的AI医疗影像诊断公司...
     *
     * @param query 搜索关键词
     * @return 格式化后的搜索结果文本；无结果时返回空字符串
     */
    public String searchAndFormat(String query) {
        List<Map<String, String>> results = search(query);
        if (results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("搜索关键词：").append(query).append("\n\n");
        sb.append("搜索结果：\n");
        for (int i = 0; i < results.size(); i++) {
            Map<String, String> result = results.get(i);
            sb.append(i + 1).append(". ");
            sb.append("标题：").append(result.getOrDefault("title", "")).append("\n");
            sb.append("   链接：").append(result.getOrDefault("link", "")).append("\n");
            sb.append("   摘要：").append(result.getOrDefault("snippet", "")).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 解析 Serper.dev API 返回的搜索结果 JSON
     *
     * 解析两部分内容：
     * 1. organic：自然搜索结果（主要数据来源）
     * 2. knowledgeGraph：Google 知识图谱（如有，插入到结果列表最前面）
     *
     * @param responseBody Serper.dev API 返回的原始 JSON 字符串
     * @return 结构化的搜索结果列表
     */
    private List<Map<String, String>> parseSearchResults(String responseBody) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // 解析自然搜索结果（organic results）
            JsonNode organic = root.get("organic");
            if (organic != null && organic.isArray()) {
                for (JsonNode item : organic) {
                    Map<String, String> result = new HashMap<>();
                    result.put("title", item.has("title") ? item.get("title").asText() : "");
                    result.put("link", item.has("link") ? item.get("link").asText() : "");
                    result.put("snippet", item.has("snippet") ? item.get("snippet").asText() : "");
                    results.add(result);
                }
            }

            // 解析知识图谱（knowledgeGraph），如有则插入到列表最前面
            // 知识图谱通常包含更权威的摘要信息
            JsonNode knowledgeGraph = root.get("knowledgeGraph");
            if (knowledgeGraph != null && !knowledgeGraph.isEmpty()) {
                Map<String, String> kgResult = new HashMap<>();
                kgResult.put("title", knowledgeGraph.has("title") ? knowledgeGraph.get("title").asText() : "");
                kgResult.put("link", knowledgeGraph.has("website") ? knowledgeGraph.get("website").asText() : "");
                kgResult.put("snippet", knowledgeGraph.has("description") ? knowledgeGraph.get("description").asText() : "");
                // 只有标题非空时才添加，避免插入无效数据
                if (!kgResult.get("title").isEmpty()) {
                    results.add(0, kgResult);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse Serper search results", e);
        }
        return results;
    }
}
