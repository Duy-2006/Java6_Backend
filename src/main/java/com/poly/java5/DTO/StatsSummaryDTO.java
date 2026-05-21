package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsSummaryDTO {
	private BigDecimal totalRevenue;
    private Double revenueGrowth;
    private Long totalOrders;
    private Double orderGrowth;
    private BigDecimal avgOrderValue;
    private Double avgGrowth;
    private Long totalCustomers;
    private Double customerGrowth;
}
