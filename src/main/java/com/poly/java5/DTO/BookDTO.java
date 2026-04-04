package com.poly.java5.DTO;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BookDTO {
	private Integer id;
    private String title;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;

    private String categoryName;
    private String authorName;
}
