package com.tracker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tracker.entity.SeedAgent;
import com.tracker.mapper.SeedAgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 种子智能体库控制器
 *
 * 提供种子智能体的增删改查API：
 * - GET    /api/seeds           查询所有种子智能体
 * - POST   /api/seeds           新增种子智能体
 * - PUT    /api/seeds/{id}      更新种子智能体
 * - DELETE /api/seeds/{id}      删除种子智能体
 * - PATCH  /api/seeds/{id}/toggle  切换启用/禁用状态
 *
 * 种子库的作用：
 * 滚雪球搜索时，系统会将种子库中的产品作为固定起点，
 * 与已发现的产品合并后交给AI，让AI基于这些种子联想出更多同类产品。
 * 种子库降低了滚雪球搜索的随机性，确保每次搜索都有稳定的发现能力。
 */
@RestController
@RequestMapping("/api/seeds")
@RequiredArgsConstructor
public class SeedAgentController {

    private final SeedAgentMapper seedAgentMapper;

    /**
     * 查询所有种子智能体
     * 按细分领域排序返回
     *
     * @return 种子智能体列表
     */
    @GetMapping
    public List<SeedAgent> list() {
        return seedAgentMapper.selectList(
                new LambdaQueryWrapper<SeedAgent>()
                        .orderByAsc(SeedAgent::getCategory));
    }

    /**
     * 新增种子智能体
     *
     * @param seed 种子智能体信息（不含id和createdAt）
     * @return 新增后的种子智能体（含自动生成的id）
     */
    @PostMapping
    public SeedAgent create(@RequestBody SeedAgent seed) {
        seedAgentMapper.insert(seed);
        return seed;
    }

    /**
     * 更新种子智能体
     *
     * @param id   种子ID
     * @param seed 更新内容
     * @return 更新后的种子智能体
     */
    @PutMapping("/{id}")
    public SeedAgent update(@PathVariable Long id, @RequestBody SeedAgent seed) {
        seed.setId(id);
        seedAgentMapper.updateById(seed);
        return seedAgentMapper.selectById(id);
    }

    /**
     * 删除种子智能体
     *
     * @param id 种子ID
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        seedAgentMapper.deleteById(id);
    }

    /**
     * 切换种子智能体的启用/禁用状态
     *
     * @param id 种子ID
     * @return 更新后的种子智能体
     */
    @PatchMapping("/{id}/toggle")
    public SeedAgent toggle(@PathVariable Long id) {
        SeedAgent seed = seedAgentMapper.selectById(id);
        if (seed != null) {
            seed.setEnabled(!seed.getEnabled());
            seedAgentMapper.updateById(seed);
        }
        return seed;
    }
}
