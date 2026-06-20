package com.tracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 跨域配置类
 *
 * 允许前端（Vue 3 运行在 localhost:5173）跨域访问后端 API（localhost:8080）
 * 配置范围：所有 /api/** 路径
 * 允许的 HTTP 方法：GET、POST、PUT、DELETE
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域映射规则
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}
