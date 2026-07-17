package com.poly.java5.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "payos")
@Data
public class PayOSConfig {
    // Gán giá trị mặc định là "none" ngay tại đây
    private String clientId = "none";
    private String apiKey = "none";
    private String checksumKey = "none";
    private String baseUrl = "none";
}