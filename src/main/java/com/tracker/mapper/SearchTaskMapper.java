package com.tracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tracker.entity.SearchTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索任务 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，提供搜索任务表的 CRUD 操作
 * 对应数据库表：search_task
 */
@Mapper
public interface SearchTaskMapper extends BaseMapper<SearchTask> {
}
