package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueDTO {
	private String month;   // "T1", "T2"
    private BigDecimal revenue;
    private Long orders;
}
