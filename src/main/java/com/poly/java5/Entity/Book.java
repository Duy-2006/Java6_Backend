package com.poly.java5.Entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Entity
@Table(name = "Books")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  // ✅ THÊM DÒNG NÀY
public class Book implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên sách không được để trống")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 20)
    private String isbn;

    @Column(precision = 10, scale = 2)
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

    // ✅ THÊM @JsonBackReference ĐỂ TRÁNH VÒNG LẶP
    @ManyToOne
    @JoinColumn(name = "author_id")
    @JsonBackReference  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Author author;

    // ✅ THÊM @JsonBackReference ĐỂ TRÁNH VÒNG LẶP
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonBackReference  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @JsonIgnore  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User seller;

    // ✅ THÊM @JsonIgnore CHO CÁC LIST
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews; 

    @OneToMany(mappedBy = "book")
    @JsonIgnore  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CartDetail> cartDetails; 

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonIgnore  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderDetail> orderDetails; 

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonIgnore  // THÊM DÒNG NÀY
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Wishlist> wishlists; 
    
 //  THÊM QUAN HỆ VỚI BẢNG CHƯƠNG SÁCH CHO TÍNH NĂNG AUDIO
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Dùng Ignore để tránh Next.js load quá nặng khi chỉ xem danh sách sách
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BookChapter> chapters;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

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
    
    // Getter và setter cho tempDiscountPercent
    public BigDecimal getTempDiscountPercent() {
        return tempDiscountPercent;
    }

    public void setTempDiscountPercent(BigDecimal tempDiscountPercent) {
        this.tempDiscountPercent = tempDiscountPercent;
    }
}