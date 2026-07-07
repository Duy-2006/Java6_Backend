package com.poly.java5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   
public class WebBanSachOnlikeApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebBanSachOnlikeApplication.class, args);
	}

}

