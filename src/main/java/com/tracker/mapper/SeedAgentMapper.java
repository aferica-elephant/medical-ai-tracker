package com.tracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tracker.entity.SeedAgent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 种子智能体 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，提供种子智能体表的 CRUD 操作
 * 对应数据库表：seed_agent
 */
@Mapper
public interface SeedAgentMapper extends BaseMapper<SeedAgent> {
}
