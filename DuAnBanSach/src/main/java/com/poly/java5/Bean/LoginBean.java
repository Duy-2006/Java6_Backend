package com.poly.java5.Bean;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginBean {
	 @NotBlank(message = "Tên đăng nhập không được để trống")
	    private String usernameOrEmail;
	    
	    @NotBlank(message = "Mật khẩu không được để trống")
	    private String password;
	    
	    private Boolean rememberMe = false;
}
