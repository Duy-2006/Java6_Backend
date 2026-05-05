package com.poly.java5.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class InventoryLogDTO {
	 private Long id;
	    private Long bookId;
	    private String bookTitle;
	    private Integer changeAmount;   // thay đổi số lượng (dương = nhập, âm = xuất)
	    private String type;            // "IMPORT" hoặc "EXPORT"
	    private String note;
	    private LocalDateTime logDate;
}
