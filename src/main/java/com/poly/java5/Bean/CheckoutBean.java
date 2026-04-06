package com.poly.java5.Bean;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CheckoutBean {
	@NotBlank(message = "Họ tên không được để trống")
	private String customerName;

	@NotBlank(message = "Số điện thoại không được để trống")
	@Pattern(regexp = "^(0[0-9]{9})$",
		    message = "Số điện thoại phải đủ 10 số và bắt đầu bằng 0")
	private String customerPhone;

	@NotBlank(message = "Email không được để trống")
	@Email(message = "Email không đúng định dạng")
	private String email;

	@NotBlank(message = "Địa chỉ không được để trống")
	private String customerAddress;

	@NotBlank(message = "Vui lòng chọn phương thức thanh toán")
	private String paymentMethod;
}
