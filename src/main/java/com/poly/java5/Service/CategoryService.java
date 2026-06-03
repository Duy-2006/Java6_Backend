package com.poly.java5.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.DTO.CategoryDetailDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;
import com.poly.java5.Repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {
	 private final CategoryRepository categoryRepository;

	    public CategoryService(CategoryRepository categoryRepository) {
	        this.categoryRepository = categoryRepository;
	    }

	    // LẤY TẤT CẢ CATEGORY
	    public List<Category> findAll() {
	        return categoryRepository.findAll();
	    }

	    // LẤY CATEGORY THEO ID (trả về Optional)
	    public Optional<Category> findById(Integer id) {
	        return categoryRepository.findById(id);
	    }
	    
	    // LẤY CATEGORY THEO ID (ném exception nếu không tìm thấy)
	    public Category getById(Integer id) {
	        return categoryRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với id: " + id));
	    }
	    
	    // XÓA CATEGORY
	    @Transactional
	    public void deleteById(Integer id) {
	        if (categoryRepository.existsById(id)) {
	            categoryRepository.deleteById(id);
	        } else {
	            throw new RuntimeException("Không thể xóa - Không tìm thấy thể loại với id: " + id);
	        }
	    }
	    
	    // TẠO MỚI HOẶC CẬP NHẬT CATEGORY
	    @Transactional
	    public Category save(Category category) {
	        if (category == null) {
	            throw new IllegalArgumentException("Category không được null");
	        }
	        if (category.getName() == null || category.getName().trim().isEmpty()) {
	            throw new IllegalArgumentException("Tên category không được để trống");
	        }
	        return categoryRepository.save(category);
	    }
	    
	    // KIỂM TRA TỒN TẠI
	    public boolean existsById(Integer id) {
	        return categoryRepository.existsById(id);
	    }
	    
	    // ĐẾM SỐ LƯỢNG
	    public long count() {
	        return categoryRepository.count();
	    }
	    
	    // ========== METHOD MỚI ==========
	    public List<CategoryDetailDTO> findAllWithBooks() {
	        List<Category> categories = categoryRepository.findAllWithBooks();
	        return categories.stream()
	                .map(this::convertToCategoryDetailDTO)
	                .collect(Collectors.toList());
	    }

	    private CategoryDetailDTO convertToCategoryDetailDTO(Category category) {
	        List<BookDTO> bookDTOs = category.getBooks().stream()
	                .map(this::convertToBookDTO)
	                .collect(Collectors.toList());
	        
	        // Dùng constructor hoặc setter phù hợp
	        CategoryDetailDTO dto = new CategoryDetailDTO();
	        dto.setId(category.getId());
	        dto.setName(category.getName());
	        dto.setImageUrl(category.getImageUrl());   // <-- THÊM DÒNG NÀY
	        dto.setBooks(bookDTOs);
	        dto.setBookCount((long) bookDTOs.size());
	        return dto;
	    }

	    private BookDTO convertToBookDTO(Book book) {
	        // Lấy thông tin author và category (nếu có)
	        Long authorId = null;
	        String authorName = null;
	        if (book.getAuthor() != null) {
	            authorId = book.getAuthor().getId();
	            authorName = book.getAuthor().getName();
	        }
	        
	        Integer categoryId = null;
	        String categoryName = null;
	        if (book.getCategory() != null) {
	            categoryId = book.getCategory().getId();
	            categoryName = book.getCategory().getName();
	        }
	        
	        BookDTO dto = new BookDTO();
	        dto.setId(book.getId());
	        dto.setTitle(book.getTitle());
	        dto.setIsbn(book.getIsbn());
	        dto.setPublisher(book.getPublisher());
	        dto.setPrice(book.getPrice());
	        dto.setQuantity(book.getQuantity());
	        dto.setActive(book.getActive());
	        dto.setDescription(book.getDescription());
	        dto.setImageUrl(book.getImageUrl());
	        dto.setAuthorId(authorId);
	        dto.setAuthorName(authorName);
	        dto.setCategoryId(categoryId);
	        dto.setCategoryName(categoryName);
	        return dto;
	    }
}
