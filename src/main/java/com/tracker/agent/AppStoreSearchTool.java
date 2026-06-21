package com.tracker.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * App Store 官方搜索工具
 *
 * 供 AgentScope 智能体调用的工具，使用 Apple 官方 iTunes Search API
 * 搜索 App Store 中的应用信息。
 *
 * iTunes Search API 特点：
 * - 官方免费 API，无需认证
 * - 返回结构化 JSON 数据
 * - 包含应用名称、开发者、评分、评分人数、价格、描述、分类等字段
 * - 支持按国家/地区搜索（cn=中国区）
 *
 * API 端点：https://itunes.apple.com/search
 * 参数：term=关键词, country=cn, media=software, entity=software, limit=20
 */
@Slf4j
public class AppStoreSearchTool {

    /** iTunes Search API 端点 */
    private static final String ITUNES_SEARCH_URL = "https://itunes.apple.com/search";

    /** 共享 OkHttpClient 实例（连接池复用，避免每次请求新建） */
    private static final OkHttpClient SHARED_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 搜索 App Store 中的应用
     *
     * 智能体在推理过程中，当需要查找应用商店中的应用信息时，
     * 会自动调用此工具。返回结构化的应用数据，包括：
     * - 应用名称、开发者
     * - 用户评分、评分人数
     * - 价格（免费/付费）
     * - 应用描述
     * - App Store 链接
     * - 主要分类
     *
     * @param query 搜索关键词，如"医疗AI"、"AI问诊"、"平安好医生"
     * @return 格式化后的应用搜索结果文本
     */
    @Tool(name = "app_store_search", description = "搜索苹果 App Store 中的应用，获取应用名称、开发者、评分、评分人数、价格、描述等信息。当需要查找应用商店数据时使用此工具。")
    public String search(
            @ToolParam(name = "query", description = "搜索关键词，如'医疗AI'、'AI问诊'、'平安好医生'、'健康'") String query) {
        try {
            // 构建iTunes Search API请求URL
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s?term=%s&country=cn&media=software&entity=software&limit=20&lang=zh_cn",
                    ITUNES_SEARCH_URL, encodedQuery);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = SHARED_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (response.code() == 429) {
                        // Apple限流，等待10秒后重试一次
                        log.warn("iTunes API rate limited (429), retrying after 10s...");
                        Thread.sleep(10000);
                        try (Response retryResponse = SHARED_CLIENT.newCall(request).execute()) {
                            if (!retryResponse.isSuccessful()) {
                                log.error("iTunes Search API retry failed: status={}", retryResponse.code());
                                return "App Store搜索失败：重试后仍返回错误码 " + retryResponse.code();
                            }
                            String retryBody = retryResponse.body().string();
                            return parseResults(retryBody, query);
                        }
                    }
                    log.error("iTunes Search API failed: status={}", response.code());
                    return "App Store搜索失败：API返回错误码 " + response.code();
                }
                String responseBody = response.body().string();
                return parseResults(responseBody, query);
            }
        } catch (Exception e) {
            log.error("App Store search error for query: {}", query, e);
            return "App Store搜索失败：" + e.getMessage();
        }
    }

    /**
     * 解析 iTunes Search API 返回的 JSON 结果
     *
     * 提取每个应用的关键信息并格式化为文本：
     * - trackName: 应用名称
     * - sellerName: 开发者/公司
     * - averageUserRating: 用户评分（0-5）
     * - userRatingCount: 评分人数
     * - formattedPrice: 价格（"Free"或具体金额）
     * - description: 应用描述
     * - trackViewUrl: App Store链接
     * - primaryGenreName: 主要分类
     *
     * @param responseBody API 返回的原始 JSON
     * @param query        搜索关键词
     * @return 格式化后的搜索结果文本
     */
    private String parseResults(String responseBody, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("App Store搜索关键词：").append(query).append("\n\n");

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            int resultCount = root.has("resultCount") ? root.get("resultCount").asInt() : 0;
            sb.append("搜索结果数量：").append(resultCount).append("\n\n");

            JsonNode results = root.get("results");
            if (results != null && results.isArray()) {
                int index = 1;
                for (JsonNode app : results) {
                    String trackName = getTextOrDefault(app, "trackName", "");
                    String sellerName = getTextOrDefault(app, "sellerName", "");
                    String price = getTextOrDefault(app, "formattedPrice", "");
                    String rating = app.has("averageUserRating")
                            ? String.format("%.1f", app.get("averageUserRating").asDouble()) : "暂无";
                    String ratingCount = app.has("userRatingCount")
                            ? formatNumber(app.get("userRatingCount").asLong()) : "暂无";
                    String genre = getTextOrDefault(app, "primaryGenreName", "");
                    String desc = getTextOrDefault(app, "description", "");
                    // 截断过长的描述
                    if (desc.length() > 200) {
                        desc = desc.substring(0, 200) + "...";
                    }
                    String url = getTextOrDefault(app, "trackViewUrl", "");

                    sb.append(index).append(". 应用名：").append(trackName).append("\n");
                    sb.append("   开发者：").append(sellerName).append("\n");
                    sb.append("   评分：").append(rating).append("/5 (").append(ratingCount).append("人评分)\n");
                    sb.append("   价格：").append(price).append("\n");
                    sb.append("   分类：").append(genre).append("\n");
                    sb.append("   简介：").append(desc).append("\n");
                    sb.append("   链接：").append(url).append("\n\n");
                    index++;
                }
            }

            if (resultCount == 0) {
                sb.append("未找到相关应用。\n");
            }

        } catch (Exception e) {
            log.error("Failed to parse iTunes Search API results", e);
            sb.append("解析搜索结果失败\n");
        }

        return sb.toString();
    }

    /**
     * 安全获取JSON节点的文本值
     *
     * @param node        JSON节点
     * @param fieldName   字段名
     * @param defaultValue 默认值
     * @return 字段文本值或默认值
     */
    private String getTextOrDefault(JsonNode node, String fieldName, String defaultValue) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : defaultValue;
    }

    /**
     * 格式化数字，如 12345 → "1.2万"
     *
     * @param number 原始数字
     * @return 格式化后的字符串
     */
    private String formatNumber(long number) {
        if (number >= 100000000) {
            return String.format("%.1f亿", number / 100000000.0);
        } else if (number >= 10000) {
            return String.format("%.1f万", number / 10000.0);
        }
        return String.valueOf(number);
    }
}
