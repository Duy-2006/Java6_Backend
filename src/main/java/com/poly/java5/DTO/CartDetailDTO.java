package com.poly.java5.DTO;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CartDetailDTO {
	private Integer id;
    private Integer quantity;
    private BigDecimal price;
    private Boolean selected;

    private Integer bookId;
    private String bookTitle;
    private String imageUrl;
}
