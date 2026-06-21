package com.tracker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tracker.entity.SearchKeywordConfig;
import com.tracker.mapper.SearchKeywordConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索关键词配置控制器
 *
 * 提供搜索关键词配置的增删改查API：
 * - GET    /api/keywords           查询所有关键词配置（按领域和轮次排序）
 * - POST   /api/keywords           新增关键词配置
 * - PUT    /api/keywords/{id}      更新关键词配置
 * - DELETE /api/keywords/{id}      删除关键词配置
 * - PATCH  /api/keywords/{id}/toggle  切换启用/禁用状态
 */
@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordConfigController {

    private final SearchKeywordConfigMapper keywordConfigMapper;

    /**
     * 查询所有关键词配置
     * 按领域（category）和轮次（round）排序返回
     *
     * @return 关键词配置列表
     */
    @GetMapping
    public List<SearchKeywordConfig> list() {
        return keywordConfigMapper.selectList(
                new LambdaQueryWrapper<SearchKeywordConfig>()
                        .orderByAsc(SearchKeywordConfig::getCategory)
                        .orderByAsc(SearchKeywordConfig::getRound));
    }

    /**
     * 新增关键词配置
     *
     * @param config 关键词配置（不含id和createdAt）
     * @return 新增后的配置（含自动生成的id）
     */
    @PostMapping
    public SearchKeywordConfig create(@RequestBody SearchKeywordConfig config) {
        keywordConfigMapper.insert(config);
        return config;
    }

    /**
     * 更新关键词配置
     * 只更新非空字段
     *
     * @param id     配置ID
     * @param config 更新内容
     * @return 更新后的配置
     */
    @PutMapping("/{id}")
    public SearchKeywordConfig update(@PathVariable Long id, @RequestBody SearchKeywordConfig config) {
        config.setId(id);
        keywordConfigMapper.updateById(config);
        return keywordConfigMapper.selectById(id);
    }

    /**
     * 删除关键词配置
     *
     * @param id 配置ID
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        keywordConfigMapper.deleteById(id);
    }

    /**
     * 切换关键词的启用/禁用状态
     * 将 enabled 字段取反
     *
     * @param id 配置ID
     * @return 更新后的配置
     */
    @PatchMapping("/{id}/toggle")
    public SearchKeywordConfig toggle(@PathVariable Long id) {
        SearchKeywordConfig config = keywordConfigMapper.selectById(id);
        if (config != null) {
            config.setEnabled(!config.getEnabled());
            keywordConfigMapper.updateById(config);
        }
        return config;
    }
}
