package com.poly.java5.DTO;
import lombok.Data;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderFullDTO {
	 private Integer id;
	    private String orderCode;
	    private String status;
	    private String customerName;
	    private String customerPhone;
	    private String customerAddress;
	    private BigDecimal totalAmount;
	    private String paymentMethod;
	    private String paymentStatus;
	    private LocalDateTime orderDate;
	    private List<OrderDetailDTO> details;
	    private List<OrderDetailDTO> orderDetails; // Nếu frontend dùng tên này
		

}
