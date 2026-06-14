package com.poly.java5.DTO;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentRequestDTO {
    private long orderCode;
    private int amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private List<OrderItemDTO> items;
}


