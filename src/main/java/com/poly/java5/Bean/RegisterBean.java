package com.poly.java5.Bean;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
	
	@Email(message = "Email không đúng định dạng")
    @NotBlank(message = "email không được bỏ trống")
	private String email;
	
	@NotBlank(message = "phone không được bỏ trống")
    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "SĐT không hợp lệ")
	private String phone;
	
	@NotBlank(message = "Vui lòng xác nhận mật khẩu")
	private String confirmPassword;
}
