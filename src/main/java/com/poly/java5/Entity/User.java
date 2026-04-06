package com.poly.java5.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
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

    // ===== BUSINESS LOGIC (THỐNG KÊ CHI TIÊU) =====
    public Double getTotalSpending() {
        if (orders == null || orders.isEmpty()) return 0.0;
        
        // Tính tổng tiền các đơn hàng đã hoàn thành
        return orders.stream()
                .filter(o -> o.getStatus() != null && "COMPLETED".equalsIgnoreCase(o.getStatus()))
                .filter(o -> o.getTotalAmount() != null) // Tránh lỗi null pointer
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum();
    }

    public String getCustomerType() {
        double spent = getTotalSpending();
        if (spent >= 5_000_000) return "VIP (Thân thiết)";
        if (spent > 0) return "Tiềm năng";
        return "Khách mới";
    }

	

	
}