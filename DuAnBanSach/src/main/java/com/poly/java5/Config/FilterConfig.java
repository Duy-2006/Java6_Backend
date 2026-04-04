package com.poly.java5.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class FilterConfig {
	 private final AuthFilter authFilter;

	    public FilterConfig(AuthFilter authFilter) {
	        this.authFilter = authFilter;
	    }

	    @Bean
	    public FilterRegistrationBean<AuthFilter> authFilterRegistration() {
	        FilterRegistrationBean<AuthFilter> registration =
	                new FilterRegistrationBean<>();

	        registration.setFilter(authFilter);
	        registration.addUrlPatterns("/*"); // bắt tất cả
	        registration.setOrder(1);

	        return registration;
	    }
}
