package com.poly.java5.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import com.poly.java5.Entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Hiện tại chưa cần viết thêm hàm gì, JpaRepository đã có đủ CRUD rồi
}