package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {
	private String token;         // JWT
    private UserDTO user;    // thông tin user (fullName, role)
}
