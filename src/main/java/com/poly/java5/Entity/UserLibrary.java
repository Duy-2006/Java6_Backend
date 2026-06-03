package com.poly.java5.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "User_Library", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "book_id", "variant_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private BookFormat variant;

    @Column(length = 50)
    private String status = "ACTIVE"; // ACTIVE, REVOKED, ARCHIVED

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt = LocalDateTime.now();
}
