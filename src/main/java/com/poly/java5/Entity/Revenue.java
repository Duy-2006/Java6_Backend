package com.poly.java5.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.autoconfigure.AutoConfiguration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AutoConfiguration
@Table(name = "revenues")
public class Revenue {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @NotNull(message = "Ngày không được để trống")
	    @Column(nullable = false, unique = true)
	    private LocalDate date;

	    @NotNull(message = "Doanh thu không được để trống")
	    @PositiveOrZero(message = "Doanh thu phải >= 0")
	    @Column(name = "total_revenue", nullable = false, precision = 15, scale = 2)
	    private BigDecimal totalRevenue;

    // Liên kết với seller hoặc order nếu cần
}
