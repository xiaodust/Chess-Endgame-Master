package com.example.chess.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 所有接口
                .allowedOriginPatterns(
                        "*",
                        "null",
                        "file://*",
                        "http://*",
                        "https://*",
                        "http://10.0.2.2:*",
                        "http://localhost:*",
                        "http://192.168.*:*" // 允许局域网与模拟器
                ) // 允许localhost和局域网访问
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头
                .allowCredentials(false) // 使用通配符放开来源时禁用凭证
                .maxAge(3600); // 预检请求的有效期
    }
}
