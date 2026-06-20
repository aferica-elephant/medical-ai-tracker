package com.tracker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tracker.entity.SearchKeywordConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索关键词配置 Mapper 接口
 *
 * 继承 MyBatis-Plus BaseMapper，提供搜索关键词配置表的 CRUD 操作
 * 对应数据库表：search_keyword_config
 */
@Mapper
public interface SearchKeywordConfigMapper extends BaseMapper<SearchKeywordConfig> {
}
