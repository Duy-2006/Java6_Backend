package com.poly.java5.Entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString; // Đã thêm import này

@Data
@Entity
@Table(name = "authors")
public class Author implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên tác giả không được để trống")
    private String name;

    private String email; // Thông tin thêm nếu cần

    // Quan hệ 1-Nhiều với Sách để thống kê
    @OneToMany(mappedBy = "author")
    @ToString.Exclude // <--- Đã thêm để ngắt vòng lặp với danh sách Book
    private List<Book> books;
}