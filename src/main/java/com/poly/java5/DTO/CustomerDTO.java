package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
	 private String username;
	    private String fullName;
	    private String email;
	    private String phone;
	    private Boolean active;
	    private Double totalSpending;
	    private String customerType;
}
