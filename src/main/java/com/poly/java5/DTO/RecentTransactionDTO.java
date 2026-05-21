package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class RecentTransactionDTO {
	private String orderCode;
    private String customerName;
    private BigDecimal amount;
    private String status;
    private LocalDate date;
}
