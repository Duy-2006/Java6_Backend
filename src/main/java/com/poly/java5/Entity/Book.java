package com.poly.java5.Entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Entity
@Table(name = "Books")
public class Book implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Giữ lại kiểm tra Tên sách vì đây là trường bắt buộc tối thiểu
    @NotBlank(message = "Tên sách không được để trống")
    @Column(nullable = false, length = 200)
    private String title;

    // --- ĐÃ TẠM TẮT VALIDATION ĐỂ TRÁNH LỖI TRANSACTION KHI CẬP NHẬT KHO ---
    // @NotBlank(message = "Mã ISBN không được để trống")
    @Column(length = 20)
    private String isbn;

    // @NotNull(message = "Giá bán không được để trống")
    // @Min(value = 1000, message = "Giá bán phải từ 1.000 VNĐ trở lên")
    @Column(precision = 10, scale = 2) // Bỏ nullable = false để cho phép dữ liệu cũ
    private BigDecimal price; 

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    @Column(name = "stock_quantity", nullable = false)
    private Integer quantity;

    @Column(length = 100)
    private String publisher;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;

    private Boolean active = true;  
    private Boolean deleted = false; 

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    
    @Transient
    private BigDecimal tempDiscountPercent;

    public BigDecimal getTempDiscountPercent() {
        return tempDiscountPercent;
    }

    public void setTempDiscountPercent(BigDecimal tempDiscountPercent) {
        this.tempDiscountPercent = tempDiscountPercent;
    }

    // --- ĐÃ TẠM TẮT VALIDATION QUAN HỆ ĐỂ TRÁNH LỖI TRANSACTION ---
    // @NotNull(message = "Vui lòng chọn tác giả")
    @ManyToOne
    @JoinColumn(name = "author_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Author author;

    // @NotNull(message = "Vui lòng chọn thể loại")
    @ManyToOne
    @JoinColumn(name = "category_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User seller;

    // 5. CÁC LIST QUAN HỆ (ONE-TO-MANY)
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews; 

    @OneToMany(mappedBy = "book")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CartDetail> cartDetails; 

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderDetail> orderDetails; 

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Wishlist> wishlists; 

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    // 7. NGHIỆP VỤ (BUSINESS LOGIC)
    
    public boolean isAvailable() {
        return quantity != null && quantity > 0 && Boolean.TRUE.equals(active);
    }

    public void decreaseStock(Integer amount) {
        if (this.quantity < amount) {
            throw new IllegalArgumentException("Kho không đủ hàng!");
        }
        this.quantity -= amount;
    }

    public void increaseStock(Integer amount) {
        if (amount > 0) {
            this.quantity += amount;
        }
    }

    public BigDecimal calculateTotalPrice(Integer quantity) {
        if (price == null || quantity == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}