package com.poly.java5.DTO;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ImportRequestDTO {
	 @NotNull(message = "bookId không được để trống")
	    private Long bookId;

	    @NotNull(message = "Số lượng không được để trống")
	    @Min(value = 1, message = "Số lượng nhập phải >= 1")
	    private Integer quantity;

	    private String note;
}
