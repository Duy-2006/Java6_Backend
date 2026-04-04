package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResetPasswordRequestDTO {
	private String email;
    private String otp;
    private String newPassword;
}
