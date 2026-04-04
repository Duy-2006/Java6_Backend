package com.poly.java5.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "InventoryLogs") // <--- QUAN TRỌNG: Khớp 100% với tên bảng SQL Server
public class InventoryLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "change_amount") // Khớp cột SQL 'change_amount'
    private Integer changeAmount;

    @Column(length = 20)
    private String type; // IMPORT / EXPORT

    @Column(length = 255)
    private String note;

    @Column(name = "log_date") // Khớp cột SQL 'log_date'
    private LocalDateTime logDate;

    @PrePersist
    public void onCreate() {
        if (logDate == null) {
            logDate = LocalDateTime.now();
        }
    }
}