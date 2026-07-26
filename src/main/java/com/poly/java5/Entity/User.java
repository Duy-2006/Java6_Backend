package com.poly.java5.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.math.BigDecimal;

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
    private String fullName;

    @Column(length = 20)
    private String phone;

    // --- QUAN TRỌNG NHẤT: BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ FIX LỖI QUYỀN HẠN ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER; // Mặc định là USER

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    // ===== RELATIONSHIP (QUAN HỆ) =====
    // mappedBy = "user" phải khớp với tên biến 'user' trong class Order
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @ToString.Exclude          // Lombok: Tránh vòng lặp vô hạn khi in log
    @EqualsAndHashCode.Exclude // Lombok: Tránh lỗi so sánh object
    
    @Column(name = "lifetime_value", precision = 12, scale = 2)
    private BigDecimal lifetimeValue = BigDecimal.ZERO;

    @Column(name = "customer_rank", length = 20)
    private String customerRank = "BRONZE";
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

    public String calculateRank() {

        if (lifetimeValue == null) {
            return "BRONZE";
        }

        if (lifetimeValue.compareTo(new BigDecimal("10000000")) >= 0)
            return "PLATINUM";

        if (lifetimeValue.compareTo(new BigDecimal("5000000")) >= 0)
            return "GOLD";

        if (lifetimeValue.compareTo(new BigDecimal("2000000")) >= 0)
            return "SILVER";

        return "BRONZE";
    }

    public int getDiscountPercent() {

        switch (customerRank) {

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