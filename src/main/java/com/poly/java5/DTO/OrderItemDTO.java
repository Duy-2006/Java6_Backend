package com.poly.java5.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {

    // Tên sản phẩm
    private String name;

    // Số lượng
    private Integer quantity;

    // Đơn giá (VNĐ)
    private Integer price;
}