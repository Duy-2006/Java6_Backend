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

    @Column(name = "discount_type", nullable = false)
    private String discountType = "PERCENT";

    @Column(name = "discount_value", nullable = false)
    private Double discountValue;

    @Column(name = "minoder_value", nullable = false)   // theo ảnh
    private Double minOrderValue = 0.0;

    @Column(name = "max_discount")
    private Double maxDiscount;

    @Column(name = "quantity", nullable = false)       // thay vì usage_limit
    private Integer usageLimit = 100;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)         // thay vì active
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
