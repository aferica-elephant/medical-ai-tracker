package com.tracker.service;

/**
 * Prompt 模板工具类
 *
 * 集中管理所有发送给 AI 大模型的提示词模板，确保：
 * 1. 提示词格式统一，输出要求一致（JSON 数组格式）
 * 2. 不同搜索场景使用不同的模板，提高提取准确率
 * 3. 便于后续维护和优化提示词
 *
 * 模板分类：
 * - 联网搜索模板（WithWeb）：AI 自带 web_search 工具时使用，AI 自动搜索+分析
 * - 外部搜索模板（WithResults）：使用 Serper.dev 搜索后，将结果喂给 AI 分析
 * - 纯知识模板（ByCategory）：AI 仅基于自身知识库回答（降级方案）
 * - 滚雪球模板（Snowball）：基于已有产品列表，发现更多同类产品
 * - 详情补充模板（Enrich）：获取单个产品的详细信息
 *
 * 所有模板的输出格式统一为：
 * [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
 */
public class PromptTemplates {

    /**
     * 按细分领域搜索（纯知识库模式，降级方案）
     * 当外部搜索和联网搜索都不可用时，依赖 AI 自身知识库回答
     *
     * @param category 细分领域名称（如"AI影像诊断"）
     * @param keyword  搜索关键词
     * @return 格式化后的提示词
     */
    public static String searchByCategory(String category, String keyword) {
        return String.format("""
            你是一个医疗AI行业分析专家。请根据你的知识，找出中国国内与"%s"相关的AI智能体产品。

            搜索关键词参考：%s

            要求：
            1. 范围仅限中国大陆市场的产品
            2. 每个产品需包含：名称(name)、所属公司(company)、官网(website)、简介(description)、细分领域(category)、面向用户(targetUser)
            3. 请尽可能全面，不要遗漏任何已知产品
            4. 严格以JSON数组格式输出，不要输出其他内容
            5. website如果不确定可以留空字符串

            输出格式：
            [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
            """, category, keyword);
    }

    /**
     * 按细分领域搜索（联网搜索模式，推荐方案）
     * AI 会自动调用 web_search 工具联网搜索，然后分析搜索结果并提取产品信息
     * 适用于 doubao-seed 等支持 web_search 工具的模型
     *
     * @param category 细分领域名称（如"AI影像诊断"）
     * @param keyword  搜索关键词
     * @return 格式化后的提示词
     */
    public static String searchByCategoryWithWeb(String category, String keyword) {
        return String.format("""
            请搜索并整理中国国内与"%s"相关的AI智能体产品。请联网搜索最新信息。

            搜索关键词参考：%s

            要求：
            1. 范围仅限中国大陆市场的产品
            2. 医疗AI智能体定义：利用人工智能技术，在医疗健康领域提供智能化服务的产品/平台
            3. 覆盖场景包括但不限于：AI影像诊断、AI问诊、AI药物研发、AI辅助治疗、AI健康管理、AI中医、AI病历语音、AI检验等
            4. 每个产品需包含：名称(name)、所属公司(company)、官网(website)、简介(description)、细分领域(category)、面向用户(targetUser)
            5. 请尽可能全面，不要遗漏任何已知产品
            6. 严格以JSON数组格式输出，不要输出其他内容
            7. website如果不确定可以留空字符串

            输出格式：
            [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
            """, category, keyword);
    }

    /**
     * 带外部搜索结果的搜索模板（Serper.dev 模式）
     * 将 Serper.dev 的搜索结果作为上下文传给 AI，让 AI 从中提取产品信息
     * AI 还可结合自身知识补充搜索结果中未提及的产品
     *
     * @param category      细分领域名称
     * @param keyword       搜索关键词
     * @param searchResults Serper.dev 返回的格式化搜索结果文本
     * @return 格式化后的提示词
     */
    public static String searchWithResults(String category, String keyword, String searchResults) {
        return String.format("""
            你是一个医疗AI行业分析专家。我已通过搜索引擎搜索了"%s"相关信息，以下是搜索结果：

            ---
            %s
            ---

            请根据以上搜索结果，提取出所有属于"中国国内医疗AI智能体"的产品信息。

            要求：
            1. 只提取中国大陆市场的医疗AI产品/平台
            2. 医疗AI智能体定义：利用人工智能技术，在医疗健康领域提供智能化服务的产品/平台
            3. 覆盖场景包括但不限于：AI影像诊断、AI问诊、AI药物研发、AI辅助治疗、AI健康管理、AI中医、AI病历语音、AI检验等
            4. 每个产品需包含：名称(name)、所属公司(company)、官网(website)、简介(description)、细分领域(category)、面向用户(targetUser)
            5. 你也可以结合自身知识补充搜索结果中未提及但确实存在的国内医疗AI产品
            6. 严格以JSON数组格式输出，不要输出其他内容
            7. website如果不确定可以留空字符串

            输出格式：
            [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
            """, keyword, searchResults);
    }

    /**
     * 滚雪球搜索（联网搜索模式，推荐方案）
     * 基于已发现的产品列表，让 AI 联网搜索发现更多同类产品
     * 实现原理：AI 看到已有产品后，会搜索同赛道/竞争对手的产品
     *
     * @param existingAgents 已有产品列表，格式："产品名(公司名)、产品名(公司名)、..."
     * @return 格式化后的提示词
     */
    public static String snowballSearchWithWeb(String existingAgents) {
        return String.format("""
            请联网搜索中国国内最新的医疗AI智能体产品，找出尚未包含在以下已知列表中的产品。

            已知产品：%s

            要求：
            1. 只列出中国大陆市场的产品，且不在上述已知列表中
            2. 医疗AI智能体定义：利用人工智能技术，在医疗健康领域提供智能化服务的产品/平台
            3. 每个产品需包含：名称(name)、所属公司(company)、官网(website)、简介(description)、细分领域(category)、面向用户(targetUser)
            4. 严格以JSON数组格式输出，不要输出其他内容
            5. website如果不确定可以留空字符串

            输出格式：
            [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
            """, existingAgents);
    }

    /**
     * 滚雪球搜索（纯知识库模式，降级方案）
     * 不联网，仅依赖 AI 知识库推测同类产品
     *
     * @param existingAgents 已有产品列表
     * @return 格式化后的提示词
     */
    public static String snowballSearch(String existingAgents) {
        return String.format("""
            已知以下国内医疗AI产品：%s

            请根据你的知识，找出与这些产品同赛道或类似的其他国内医疗AI智能体产品，列出尚未包含在上述列表中的产品。

            要求：
            1. 只列出中国大陆市场的产品
            2. 每个产品需包含：名称(name)、所属公司(company)、官网(website)、简介(description)、细分领域(category)、面向用户(targetUser)
            3. 严格以JSON数组格式输出，不要输出其他内容
            4. website如果不确定可以留空字符串

            输出格式：
            [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
            """, existingAgents);
    }

    /**
     * 滚雪球搜索（带 Serper.dev 搜索结果）
     * 将外部搜索结果和已有产品列表一起传给 AI，发现更多产品
     *
     * @param existingAgents 已有产品列表
     * @param searchResults  Serper.dev 返回的格式化搜索结果文本
     * @return 格式化后的提示词
     */
    public static String snowballSearchWithResults(String existingAgents, String searchResults) {
        return String.format("""
            你是一个医疗AI行业分析专家。

            已知以下国内医疗AI产品：%s

            我已通过搜索引擎搜索了最新医疗AI产品信息，以下是搜索结果：

            ---
            %s
            ---

            请根据以上搜索结果和你的知识，找出尚未包含在已知列表中的其他国内医疗AI智能体产品。

            要求：
            1. 只列出中国大陆市场的产品，且不在上述已知列表中
            2. 每个产品需包含：名称(name)、所属公司(company)、官网(website)、简介(description)、细分领域(category)、面向用户(targetUser)
            3. 严格以JSON数组格式输出，不要输出其他内容
            4. website如果不确定可以留空字符串

            输出格式：
            [{"name":"产品名","company":"公司名","website":"官网URL","description":"简介","category":"细分领域","targetUser":"面向用户"}]
            """, existingAgents, searchResults);
    }

    /**
     * 补充单个智能体的详细信息
     * 用于对已入库的产品进行深度信息补充
     *
     * @param agentName 产品名称
     * @param company   所属公司名称
     * @return 格式化后的提示词
     */
    public static String enrichAgentDetails(String agentName, String company) {
        return String.format("""
            请获取"%s"（%s公司）的详细信息，包括：
            1. 产品的核心功能和技术特征
            2. 最新动态和运营状况
            3. 用户规模或市场地位（如有公开数据）
            4. 产品面向的目标用户群体

            请以JSON格式输出：
            {"name":"产品名","company":"公司名","techFeatures":"技术特征","description":"详细简介","targetUser":"面向用户","category":"细分领域"}
            """, agentName, company);
    }
}
