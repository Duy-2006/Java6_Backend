package com.poly.java5.DTO;
import lombok.Data;


import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderFullDTO {
	private String orderCode;
    private String status;

    private String customerName;
    private String customerPhone;
    private String customerAddress;

    private BigDecimal totalAmount;

    private List<OrderDetailDTO> details;
}
