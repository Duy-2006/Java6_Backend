package com.poly.java5.DTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VNPayIPNResponseDTO {
	private String rspCode;
    private String message;
}
