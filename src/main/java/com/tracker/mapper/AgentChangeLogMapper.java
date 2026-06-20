package com.tracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tracker.entity.AgentChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能体变更日志 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，提供变更日志表的 CRUD 操作
 * 对应数据库表：agent_change_log
 */
@Mapper
public interface AgentChangeLogMapper extends BaseMapper<AgentChangeLog> {
}
