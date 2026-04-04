package com.poly.java5.Bean;

import org.hibernate.validator.constraints.Length;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RegisterBean {
	@NotBlank(message = "username không được bỏ trống")
	private String username;
	
	@NotBlank(message = "password không được bỏ trống")
	private String password;
	
	@NotBlank(message = "name không được bỏ trống")
	private String name;
	
	@NotBlank(message = "email không được bỏ trống")
	private String email;
	
	@NotBlank(message = "phone không được bỏ trống")
	private String phone;
}
