package com.poly.java5.Entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "vouchers")
@Data
public class Voucher {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    @Column(unique = true, nullable = false, length = 20)
	    private String code;

	    // PERCENT hoặc FIXED
	    @Column(nullable = false)
	    private String discountType = "PERCENT";

	    // Giá trị giảm (% hoặc số tiền)
	    @Column(nullable = false)
	    private Double discountValue;

	    // Đơn hàng tối thiểu để áp dụng
	    @Column(nullable = false)
	    private Double minOrderValue = 0.0;

	    // Giảm tối đa (chỉ dùng khi discountType = PERCENT)
	    private Double maxDiscount;

	    // Số lượt dùng tối đa
	    @Column(nullable = false)
	    private Integer usageLimit = 100;

	    // Số lượt đã dùng
	    @Column(nullable = false)
	    private Integer usedCount = 0;

	    @Column(nullable = false)
	    private LocalDate startDate;

	    @Column(nullable = false)
	    private LocalDate endDate;

	    @Column(nullable = false)
	    private boolean active = true;

	    // ── Tính toán trạng thái ─────────────────────────────────
	    @Transient
	    public String getComputedStatus() {
	        LocalDate now = LocalDate.now();
	        if (!active)                        return "INACTIVE";
	        if (usedCount >= usageLimit)        return "EXHAUSTED";
	        if (now.isBefore(startDate))        return "UPCOMING";
	        if (now.isAfter(endDate))           return "EXPIRED";
	        return "ACTIVE";
	    }

	    // ── Tính số tiền giảm thực tế ────────────────────────────
	    @Transient
	    public Double calculateDiscount(Double orderAmount) {
	        if ("PERCENT".equals(discountType)) {
	            double discount = orderAmount * discountValue / 100;
	            if (maxDiscount != null) discount = Math.min(discount, maxDiscount);
	            return discount;
	        }
	        return Math.min(discountValue, orderAmount);
	    }
}
