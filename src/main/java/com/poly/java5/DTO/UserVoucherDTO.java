package com.poly.java5.DTO;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserVoucherDTO {
	 private Integer userVoucherId;
	    private VoucherResponseDTO voucher;
	    private Boolean isUsed;
	    private LocalDateTime usedDate;
}
