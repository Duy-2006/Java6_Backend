package com.poly.java5.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

	  @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    // Tên chương trình khuyến mãi
	    @Column(nullable = false, length = 200)
	    private String name;

	    @Column(name = "discount_value", precision = 5, scale = 2)
	    private BigDecimal discountValue;

	    // thời gian áp dụng
	 // Trong entity Promotion
	    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	    @Column(name = "start_date") 
	    private LocalDate startDate;

	    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	    @Column(name = "end_date")
	    private LocalDate endDate;

	    // đang bật hay tắt
	    @Column(name = "status")
	    private Boolean status = true;
	    
	    @Transient
	    public String getComputedStatus() {
	        if (startDate == null || endDate == null) return "UNKNOWN";
	        LocalDate today = LocalDate.now();
	        if (today.isBefore(startDate))  return "UPCOMING";
	        if (today.isAfter(endDate))     return "EXPIRED";
	        return "ACTIVE";
	    }

	    // ALL | BOOK | CATEGORY
	    @Column(name = "apply_type", length = 20)
	    private String applyType;

	    // chi tiết áp dụng cho sách / thể loại
	    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
	    private List<PromotionDetail> details = new ArrayList<>();
	    

	    @Transient
	    public BigDecimal applyDiscount(BigDecimal originalPrice) {
	        if (discountValue == null || originalPrice == null) return originalPrice;
	        BigDecimal factor = BigDecimal.ONE.subtract(
	            discountValue.divide(BigDecimal.valueOf(100))
	        );
	        return originalPrice.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
	    }



	
}

