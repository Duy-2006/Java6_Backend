package com.poly.java5.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleStatusResponseDTO {
	private String message;
    private Boolean active;
    private String username;
}
