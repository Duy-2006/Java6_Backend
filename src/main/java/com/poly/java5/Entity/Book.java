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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "Book_Publishers",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "publisher_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Publisher> publishers;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(columnDefinition = "nvarchar(MAX)")
    private String description;

    private Boolean active = true;  
    private Boolean deleted = false; 

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "book_type", length = 20)
    private String bookType;
    
    @Transient
    private BigDecimal tempDiscountPercent;

    // Relationships
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "Book_Authors",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Author> authors;

    public Author getAuthor() {
        return (authors == null || authors.isEmpty()) ? null : authors.get(0);
    }

    public void setAuthor(Author author) {
        if (this.authors == null) {
            this.authors = new java.util.ArrayList<>();
        }
        this.authors.clear();
        if (author != null) {
            this.authors.add(author);
        }
    }

    public String getPublisher() {
        return (publishers == null || publishers.isEmpty())
            ? ""
            : publishers.stream().map(Publisher::getName).collect(java.util.stream.Collectors.joining(", "));
    }

    public void setPublisher(String publisher) {
        // Dummy setter for compatibility
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User seller;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Review> reviews; 

    @OneToMany(mappedBy = "book")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CartDetail> cartDetails; 

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderDetail> orderDetails; 

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Wishlist> wishlists; 
    
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BookChapter> chapters;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }

    public boolean isAvailable() {
        return quantity != null && quantity > 0 && Boolean.TRUE.equals(active) && Boolean.FALSE.equals(deleted);
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