package com.tracker.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AgentScope 智能体工厂
 *
 * 核心职责：
 * 1. 统一创建各类 AgentScope 智能体
 * 2. 使用手动创建的 OpenAIChatModel（OpenAI 兼容格式，对接火山引擎 Ark API）
 * 3. 为不同搜索场景创建专门的智能体实例
 *
 * 智能体类型：
 * - SearchAgent: 分类搜索智能体，按领域和关键词搜索医疗AI产品
 * - SnowballAgent: 滚雪球搜索智能体，基于已有产品发现更多同类
 *
 * 所有智能体基于 ReActAgent（推理-行动范式），具备自主推理和工具调用能力。
 * 联网搜索能力由 LLM 自带的 web_search 提供，无需外部 Serper 工具。
 * App Store 搜索通过 AppStoreSearchTool（iTunes Search API）实现。
 */
@Slf4j
@Component
public class AgentFactory {

    /** Model 实例（OpenAI 兼容格式，对接火山引擎 Ark API） */
    private final Model model;

    /**
     * 构造智能体工厂
     *
     * @param model AgentScopeModelConfig 创建的 Model 实例
     */
    public AgentFactory(Model model) {
        this.model = model;
        log.info("AgentFactory initialized with model type: {}", model.getClass().getSimpleName());
    }

    /**
     * 创建分类搜索智能体
     *
     * 该智能体负责按细分领域和关键词搜索医疗AI产品。
     * 配备 AppStoreSearchTool（App Store搜索），联网搜索由 LLM 自带能力提供。
     *
     * @return ReActAgent 搜索智能体实例
     */
    public ReActAgent createSearchAgent() {
        Toolkit toolkit = createToolkit();

        return ReActAgent.builder()
                .name("MedicalAISearchAgent")
                .sysPrompt(SEARCH_SYSTEM_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    /**
     * 创建滚雪球搜索智能体
     *
     * 该智能体负责基于已有产品列表，发现更多同类产品。
     *
     * @return ReActAgent 滚雪球搜索智能体实例
     */
    public ReActAgent createSnowballAgent() {
        Toolkit toolkit = createToolkit();

        return ReActAgent.builder()
                .name("SnowballSearchAgent")
                .sysPrompt(SNOWBALL_SYSTEM_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(5)
                .build();
    }

    /**
     * 创建工具集，注册 AppStoreSearchTool
     *
     * 联网搜索由 LLM 自带的 web_search 能力提供（火山引擎 Ark API 支持），
     * App Store 搜索通过 AppStoreSearchTool（iTunes Search API）实现。
     *
     * @return Toolkit 工具集实例
     */
    private Toolkit createToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new AppStoreSearchTool());
        log.info("Agent equipped with AppStoreSearchTool (web search via LLM built-in)");
        return toolkit;
    }

    /** 分类搜索智能体的系统提示词 */
    private static final String SEARCH_SYSTEM_PROMPT = """
            你是一个医疗AI行业分析专家，擅长搜索和整理中国国内的医疗AI智能体产品信息。

            你的核心任务是：根据用户提供的搜索领域和关键词，尽可能全面地找出相关的医疗AI产品。

            工作流程：
            1. 根据关键词，使用 web_search 工具搜索相关信息
            2. 使用 app_store_search 工具搜索应用商店中的医疗AI应用，获取下载量、评分等数据
            3. 结合搜索结果和你的知识，识别出所有属于医疗AI的产品/平台
            4. 提取每个产品的结构化信息
            5. 以 JSON 数组格式输出结果

            重要规则：
            - 范围仅限中国大陆市场的产品
            - 医疗AI智能体定义：利用人工智能技术，在医疗健康领域提供智能化服务的产品/平台
            - company 必须使用中文公司名（如"蚂蚁集团"而非"Ant Group"，"科大讯飞"而非"iFlytek"）
            - category 必须从以下8个固定领域中选择一个（不要自创领域）：
              1. AI影像诊断（含CT/MRI/超声/病理/眼底/内镜等影像分析）
              2. AI问诊（含导诊/预问诊/智能问诊/在线问诊/诊疗决策等）
              3. AI药物研发（含药物发现/临床研究/CRO/Biotech等）
              4. AI辅助治疗（含手术机器人/放疗/手术规划/治疗决策等）
              5. AI健康管理（含慢病管理/健康监测/体检/母婴/生殖等）
              6. AI中医（含舌诊/面诊/脉诊/体质辨识/辨证论治/经方等）
              7. AI病历语音（含语音录入/病历生成/病历结构化/病历管理等）
              8. AI检验（含检验报告解读/智慧实验室/基因检测/病理检验等）
            - website 如果不确定可以留空字符串
            - 不要输出重复的产品，每个产品名只出现一次
            - 严格以 JSON 数组格式输出，不要输出其他内容

            输出格式：
            [{"name":"产品名","company":"中文公司名","website":"官网URL","description":"简介","category":"固定领域名","targetUser":"面向用户"}]
            """;

    /** 滚雪球搜索智能体的系统提示词 */
    private static final String SNOWBALL_SYSTEM_PROMPT = """
            你是一个医疗AI行业分析专家，擅长发现和补充同类产品。

            你的核心任务是：根据用户提供的已知产品列表，找出尚未包含在列表中的其他国内医疗AI智能体产品。

            工作流程：
            1. 分析已知产品列表，推断同赛道和同类产品
            2. 使用搜索工具搜索更多产品
            3. 只返回不在已知列表中的新产品
            4. 以 JSON 数组格式输出结果

            重要规则：
            - 只列出中国大陆市场的产品，且不在已知列表中
            - 医疗AI智能体定义：利用人工智能技术，在医疗健康领域提供智能化服务的产品/平台
            - company 必须使用中文公司名（如"蚂蚁集团"而非"Ant Group"，"科大讯飞"而非"iFlytek"）
            - category 必须从以下8个固定领域中选择一个（不要自创领域）：
              1. AI影像诊断（含CT/MRI/超声/病理/眼底/内镜等影像分析）
              2. AI问诊（含导诊/预问诊/智能问诊/在线问诊/诊疗决策等）
              3. AI药物研发（含药物发现/临床研究/CRO/Biotech等）
              4. AI辅助治疗（含手术机器人/放疗/手术规划/治疗决策等）
              5. AI健康管理（含慢病管理/健康监测/体检/母婴/生殖等）
              6. AI中医（含舌诊/面诊/脉诊/体质辨识/辨证论治/经方等）
              7. AI病历语音（含语音录入/病历生成/病历结构化/病历管理等）
              8. AI检验（含检验报告解读/智慧实验室/基因检测/病理检验等）
            - website 如果不确定可以留空字符串
            - 不要输出重复的产品，每个产品名只出现一次
            - 严格以 JSON 数组格式输出，不要输出其他内容

            输出格式：
            [{"name":"产品名","company":"中文公司名","website":"官网URL","description":"简介","category":"固定领域名","targetUser":"面向用户"}]
            """;
}
