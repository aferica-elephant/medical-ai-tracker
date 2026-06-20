package com.tracker.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 *
 * 配置两项核心功能：
 * 1. 分页插件：使 MyBatis-Plus 的 selectPage 方法自动生成分页 SQL
 * 2. 自动填充处理器：在 INSERT/UPDATE 时自动填充 createdAt、updatedAt 字段
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 分页拦截器
     * 拦截分页查询请求，自动添加 LIMIT/OFFSET 子句
     * DbType.MYSQL 指定使用 MySQL 方言生成分页 SQL
     *
     * @return 分页拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 注册自动填充处理器
     * 处理逻辑：
     * - INSERT 时：自动填充 createdAt 和 updatedAt 为当前时间
     * - UPDATE 时：自动填充 updatedAt 为当前时间
     *
     * 对应实体类中 @TableField(fill = FieldFill.INSERT) 等注解
     *
     * @return 自动填充处理器实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
