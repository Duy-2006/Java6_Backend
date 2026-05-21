package com.poly.java5.DTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplyVoucherResponseDTO {
	private Integer voucherId;
    private String code;
    private String discountType;
    private Double discount;
    private Double finalAmount;
}
