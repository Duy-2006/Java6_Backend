package com.poly.java5.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã đơn hàng gửi lên PayOS (duy nhất)
    @Column(name = "order_code", unique = true, nullable = false)
    private Long orderCode;

    // Số tiền (VNĐ)
    @Column(nullable = false)
    private Integer amount;

    // Mô tả đơn hàng
    @Column(length = 25) // PayOS giới hạn 25 ký tự
    private String description;

    // Trạng thái: PENDING / PAID / CANCELLED
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // Link checkout trả về từ PayOS
    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    // QR code URL
    @Column(name = "qr_code", length = 500)
    private String qrCode;

    // Thời gian tạo
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Thời gian thanh toán thành công
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    public enum PaymentStatus {
        PENDING,    // Chờ thanh toán
        PAID,       // Đã thanh toán
        CANCELLED   // Đã hủy
    }
}