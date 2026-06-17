package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class CustomerHistoryDTO {
	 private String username;
	    private String fullName;
	    private String email;
	    private String phone;
	    private Boolean active;
	    private Double totalSpending;
	    private List<?> orders;  // Có thể thay bằng List<OrderDTO> nếu có
	    private String avatar;
}
