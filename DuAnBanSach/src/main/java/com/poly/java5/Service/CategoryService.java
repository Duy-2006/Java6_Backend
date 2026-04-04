package com.poly.java5.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Category;
import com.poly.java5.Repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {
	private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // ===== LẤY TẤT CẢ CATEGORY =====
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    // ===== LẤY CATEGORY THEO ID =====
    public Category findById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy thể loại"));
    }
}
