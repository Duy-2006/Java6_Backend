package com.poly.java5.DTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderDTO {
	 private Integer id;
	    private String orderCode;
	    private String status;
	    private BigDecimal totalAmount;
	    private LocalDateTime orderDate;
}
