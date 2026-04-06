package com.poly.java5.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString; // Đã thêm import

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Orders")
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "order_code", nullable = false, unique = true, length = 20)
	private String orderCode;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private User user;

	@Column(name = "customer_name", nullable = false, length = 100)
	private String customerName;

	@Column(name = "customer_phone", nullable = false, length = 20)
	private String customerPhone;

	@Column(name = "customer_address", nullable = false, columnDefinition = "NVARCHAR(500)")
	private String customerAddress;

	@Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "payment_method", length = 20)
	private String paymentMethod;

	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "payment_status", length = 20)
	private String paymentStatus;

	@Column(name = "order_date")
	private LocalDateTime orderDate;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Set<OrderDetail> orderDetails;

	@PrePersist
	protected void onCreate() {
		if (orderDate == null) {
			orderDate = LocalDateTime.now();
		}
	}

	// ===== Business helpers (NHẸ) =====
	public boolean isCancellable() {
		return "PENDING".equals(status) || "CONFIRMED".equals(status);
	}

	public BigDecimal calculateTotal() {
		return orderDetails == null ? BigDecimal.ZERO
				: orderDetails.stream().map(OrderDetail::calculateSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}