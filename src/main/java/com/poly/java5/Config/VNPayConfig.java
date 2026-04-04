package com.poly.java5.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {
	 private String tmnCode;
	    private String hashSecret;
	    private String url;
	    private String returnUrl;
	    private String ipnUrl;
	    private String version = "2.1.0";
	    private String command = "pay";
	    private String currency = "VND";
	    private String locale = "vn";
	    private boolean testMode = true;
}
