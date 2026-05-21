package com.poly.java5.DTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderDTO {
	 private Integer id;
	    private String orderCode;
	    private String customerName;
	    private String customerPhone;
	    private String customerAddress;     
	    private BigDecimal totalAmount;
	    private String status;
	    private LocalDateTime orderDate;
	    private String paymentMethod;        
	    private String paymentStatus;    
	    private String cancelReason;
	    private List<OrderDetailDTO> orderDetails;
}
