package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VNPayPaymentResponseDTO {
	 private String paymentUrl;
	    private String orderId;
	    private Long amount;
}
