package com.poly.java5.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Book_Format", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"book_id", "format_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "format_type", nullable = false, length = 50)
    private String formatType; // "PHYSICAL" or "AUDIO"

    @Column(nullable = false, precision = 18, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "original_price", precision = 18, scale = 2)
    private java.math.BigDecimal originalPrice;

    @Column(nullable = false)
    private Boolean active = true;
}
