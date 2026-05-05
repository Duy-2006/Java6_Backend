package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDTO {
	private Integer Id;
	private String fullName;
    private String role;
}
