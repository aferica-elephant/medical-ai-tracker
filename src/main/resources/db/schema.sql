-- 医疗AI追踪系统 数据库建表脚本

-- 医疗AI智能体表
CREATE TABLE IF NOT EXISTS medical_ai_agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '产品名称',
    company VARCHAR(200) COMMENT '所属公司',
    website VARCHAR(500) COMMENT '官网地址',
    description TEXT COMMENT '产品描述',
    category VARCHAR(50) COMMENT '分类',
    target_user VARCHAR(50) COMMENT '目标用户',
    tech_features TEXT COMMENT '技术特点',
    download_count VARCHAR(100) COMMENT '应用商店下载量',
    app_rating VARCHAR(20) COMMENT '应用商店评分',
    app_rank VARCHAR(100) COMMENT '应用商店排名',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    first_found_date DATE COMMENT '首次发现日期',
    last_verified_date DATE COMMENT '最后验证日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医疗AI智能体表';

-- 搜索任务表
CREATE TABLE IF NOT EXISTS search_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    keyword VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    category VARCHAR(50) COMMENT '分类',
    round INT COMMENT '搜索轮次',
    result_count INT DEFAULT 0 COMMENT '结果数量',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态',
    raw_response LONGTEXT COMMENT '原始响应',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索任务表';

-- 智能体变更日志表
CREATE TABLE IF NOT EXISTS agent_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    change_type VARCHAR(20) NOT NULL COMMENT '变更类型',
    field_name VARCHAR(50) COMMENT '变更字段名',
    old_value TEXT COMMENT '旧值',
    new_value TEXT COMMENT '新值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体变更日志表';

-- 搜索关键词配置表
CREATE TABLE IF NOT EXISTS search_keyword_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    keyword VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    round INT DEFAULT 1 COMMENT '搜索轮次',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索关键词配置表';

-- 种子智能体库表（滚雪球搜索的固定种子，降低随机性）
CREATE TABLE IF NOT EXISTS seed_agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '产品名称',
    company VARCHAR(200) COMMENT '所属公司',
    category VARCHAR(50) COMMENT '细分领域',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_name_company (name, company)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种子智能体库';
