package com.poly.java5.Entity;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString; // Đã thêm import này

@SuppressWarnings("serial")
@Data
@Entity 
@Table(name = "Categories")
public class Category implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên thể loại không được để trống")
    private String name;

    @OneToMany(mappedBy = "category")
    @ToString.Exclude // <--- Đã thêm để ngăn chặn lỗi StackOverflowError
    private List<Book> books;
}