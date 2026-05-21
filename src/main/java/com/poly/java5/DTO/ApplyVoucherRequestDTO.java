package com.poly.java5.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyVoucherRequestDTO {
	 private String code;
	    private Double orderAmount;
}
