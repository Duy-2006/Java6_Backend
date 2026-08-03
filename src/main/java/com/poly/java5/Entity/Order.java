package com.poly.java5.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

	@Column(name = "order_type", length = 20)
	private String orderType;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private Set<OrderDetail> orderDetails;
	
	@Column(name = "transaction_no", length = 100)
	private String transactionNo;
	
	@Column(name = "cancel_reason", columnDefinition = "NVARCHAR(MAX)")
	private String cancelReason;

	@Column(name = "shipping_fee", precision = 10, scale = 2)
	private BigDecimal shippingFee;	

	@Column(name = "discount_amount", precision = 10, scale = 2)
	private BigDecimal discountAmount;

	@Column(name = "member_discount", precision = 10, scale = 2)
	private BigDecimal memberDiscount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "voucher_id")
	private Voucher voucher;

	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@PrePersist
	protected void onCreate() {
		if (orderDate == null) {
			orderDate = LocalDateTime.now();
		}
	}

	// ===== Business helpers =====
	public boolean isCancellable() {
		return "PENDING".equals(status) || "CONFIRMED".equals(status);
	}

	public BigDecimal calculateTotal() {
		return orderDetails == null ? BigDecimal.ZERO
				: orderDetails.stream().map(OrderDetail::calculateSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}