package com.poly.java5.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Gộp chung các địa chỉ tìm kiếm file vào cùng một handler /uploads/** để tránh bị đè ép cấu hình
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/", 
                                      "file:src/main/resources/static/uploads/",
                                      "classpath:/static/uploads/");
        
        // Phục vụ thêm tài nguyên từ thư mục static gốc nếu cần
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}