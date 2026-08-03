package com.poly.java5.DTO;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleBookDTO {
	private Integer id;
    private String title;
    private BigDecimal price;
    private String imageUrl;
    private BigDecimal discountValue;   // phần trăm giảm
    private BigDecimal discountPrice;   // giá sau giảm
    private Integer usageLimit;         // giới hạn số lượng
    private Integer quantity;           // tồn kho hiện tại
    private BigDecimal audioPrice;      // Giá sách nói
    private Long soldCount;             // số lượng đã bán
    private java.time.LocalDate endDate; // Ngày kết thúc khuyến mãi
    private Integer usedCount;          // Số lượt đã dùng
    private Integer promotionId;        // ID của chương trình khuyến mãi
}
