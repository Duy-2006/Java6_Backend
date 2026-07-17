package com.poly.java5.DTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PromotionDTO {
	 private Integer id;

	    @NotBlank(message = "Tên khuyến mãi không được để trống")
	    private String name;

	    @NotNull(message = "Giá trị giảm giá không được để trống")
	    @DecimalMin(value = "0.0", inclusive = false, message = "Giảm giá phải > 0%")
	    @DecimalMax(value = "100.0",                  message = "Giảm giá phải <= 100%")
	    private BigDecimal discountValue;           // khớp với discount_value trong entity

	    @NotNull(message = "Ngày bắt đầu không được để trống")
	    private LocalDate startDate;

	    @NotNull(message = "Ngày kết thúc không được để trống")
	    private LocalDate endDate;

	    private Boolean status = true;             // khớp với status trong entity
	    
	    private Integer usageLimit;                // giới hạn số lượng áp dụng
	    private Integer usedCount;                 // số lượng đã sử dụng

	    private String applyType;                  // ALL | BOOK | CATEGORY

	    private String computedStatus;             // UPCOMING | ACTIVE | EXPIRED | UNKNOWN — @Transient từ entity

	    // ── Quan hệ qua PromotionDetail ──────────────────────────────────────
	    private List<Integer> bookIds;             // gửi lên khi tạo / cập nhật
	    private List<String>  bookTitles;          // chỉ để hiển thị

	    private List<Integer> categoryIds;         // gửi lên khi tạo / cập nhật
	    private List<String>  categoryNames;       // chỉ để hiển thị
}
