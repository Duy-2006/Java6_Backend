package com.poly.java5.Entity;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
    
    @Email(message = "Email không hợp lệ")
    private String email; // Thông tin thêm nếu cần

    // Quan hệ Nhiều-Nhiều với Sách để thống kê
    @ManyToMany(mappedBy = "authors")
    @ToString.Exclude // <--- Đã thêm để ngắt vòng lặp với danh sách Book
    @JsonIgnore
    private List<Book> books;
}