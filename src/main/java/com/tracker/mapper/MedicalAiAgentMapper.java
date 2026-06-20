package com.tracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tracker.entity.MedicalAiAgent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 医疗AI智能体 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，自动获得以下能力：
 * - insert: 插入一条记录
 * - deleteById: 根据ID删除
 * - updateById: 根据ID更新
 * - selectById: 根据ID查询
 * - selectPage: 分页查询
 * - selectList: 列表查询
 * - selectCount: 计数查询
 *
 * 对应数据库表：medical_ai_agent
 */
@Mapper
public interface MedicalAiAgentMapper extends BaseMapper<MedicalAiAgent> {
}
