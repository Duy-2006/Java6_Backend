package com.poly.java5.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "Users")
public class User implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "full_name", length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String avatar;

    // --- QUAN TRỌNG NHẤT: BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ FIX LỖI QUYỀN HẠN ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER; // Mặc định là USER

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "reset_otp")
    private String resetOtp;  // Mã OTP 6 số

    @Column(name = "reset_otp_expiry")
    private LocalDateTime resetOtpExpiry;  // Thời gian hết hạn OTP

    @Column(name = "lifetime_value", precision = 12, scale = 2)
    private java.math.BigDecimal lifetimeValue = java.math.BigDecimal.ZERO;

    @Column(name = "customer_rank", length = 20)
    private String customerRank = "BRONZE";


    // ===== RELATIONSHIP (QUAN HỆ) =====
    // mappedBy = "user" phải khớp với tên biến 'user' trong class Order
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude          // Lombok: Tránh vòng lặp vô hạn khi in log
    @EqualsAndHashCode.Exclude // Lombok: Tránh lỗi so sánh object
    private List<Order> orders;

    // Tự động gán ngày tạo khi lưu mới
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    // ===== HELPER METHODS (KIỂM TRA QUYỀN) =====
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }


    public boolean isBuyer() {
        return role == UserRole.USER;
    }

    // ===== BUSINESS LOGIC (THỐNG KÊ CHI TIÊU VÀ HẠNG) =====
    public String calculateRank() {
        if (lifetimeValue == null) {
            return "BRONZE";
        }
        if (lifetimeValue.compareTo(new java.math.BigDecimal("10000000")) >= 0)
            return "PLATINUM";
        if (lifetimeValue.compareTo(new java.math.BigDecimal("5000000")) >= 0)
            return "GOLD";
        if (lifetimeValue.compareTo(new java.math.BigDecimal("2000000")) >= 0)
            return "SILVER";
        return "BRONZE";
    }

    public int getDiscountPercent() {
        if (customerRank == null) return 0;
        switch (customerRank.toUpperCase()) {
            case "PLATINUM":
                return 15;
            case "GOLD":
                return 10;
            case "SILVER":
                return 5;
            default:
                return 0;
        }
    }

	

	

	

	
}