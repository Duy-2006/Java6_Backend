package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class CategoryStatsDTO {
	private String categoryName;
    private Long value;   // tỷ lệ %
    private String color;
}
