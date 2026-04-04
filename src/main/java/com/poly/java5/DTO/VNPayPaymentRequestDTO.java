package com.poly.java5.DTO;
import lombok.Data;

@Data
public class VNPayPaymentRequestDTO {
	private Long amount;
    private String orderId;
    private String orderInfo;
    private String bankCode;
}
