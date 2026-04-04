package com.poly.java5.DTO;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderDetailDTO {
	 private String bookTitle;
	    private Integer quantity;
	    private BigDecimal price;
}
