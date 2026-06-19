package com.poly.java5.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Cấu hình CORS mở toàn bộ API cho Frontend chạy port 3000
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000", "http://192.168.*:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối động đến thư mục gốc của dự án Backend hiện tại
        String userDir = System.getProperty("user.dir").replace("\\", "/");
        
        // Đảm bảo đường dẫn file:// luôn kết thúc bằng dấu gạch chéo chuẩn quy định URL
        String uploadPath = "file:" + userDir + "/uploads/banners/";
        String fallbackUploadPath = "file:" + userDir + "/uploads/";

        // 1. Gộp chung cấu hình xử lý đọc ảnh cho thư mục /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        uploadPath,                 // Ưu tiên tìm trong thư mục uploads/banners/ ngoài ổ đĩa cứng
                        fallbackUploadPath,         // Tìm ở thư mục uploads/ tổng ngoài ổ đĩa cứng
                        "file:" + userDir + "/src/main/resources/static/uploads/", // Tìm trong thư mục code static (khi code local)
                        "classpath:/static/uploads/" // Tìm bên trong file .jar sau khi build đóng gói
                );
        
        // 2. Phục vụ thêm tài nguyên từ thư mục static gốc của hệ thống (ví dụ các icon, ảnh hệ thống cố định)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}