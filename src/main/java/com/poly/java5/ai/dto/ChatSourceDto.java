package com.poly.java5.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSourceDto {
    private Integer bookId;
    private String title;
    private String reason;
    private Double relevanceScore;
    private String imageUrl;
    private java.math.BigDecimal price;
    private Integer stockQuantity;
}
