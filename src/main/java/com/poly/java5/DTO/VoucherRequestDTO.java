package com.poly.java5.DTO;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoucherRequestDTO {
	 private String code;
	    private String discountType;   // PERCENT / FIXED
	    private Double discountValue;
	    private Double minOrderValue;
	    private Double maxDiscount;    // null nếu không có
	    private Integer usageLimit;
	    private LocalDate startDate;
	    private LocalDate endDate;
	    private Boolean active;
}
