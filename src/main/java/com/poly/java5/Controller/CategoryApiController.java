package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.DTO.CategoryDTO;
import com.poly.java5.DTO.CategoryDetailDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;
import com.poly.java5.Repository.CategoryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // cho Next.js gọi
public class CategoryApiController {
	 @PersistenceContext
	    private EntityManager em;

	    @Autowired
	    private CategoryRepository catRepo;

	 // ================== GET ALL (cả danh sách books + bookCount) ==================
	    @GetMapping
	    public ResponseEntity<List<CategoryDetailDTO>> getAll() {
	        // Lấy tất cả categories kèm theo danh sách books (dùng LEFT JOIN FETCH)
	        List<Category> categories = catRepo.findAllWithBooks();
	        
	        List<CategoryDetailDTO> dtos = categories.stream().map(category -> {
	            // Chuyển đổi danh sách Book -> BookDTO
	            List<BookDTO> bookDTOs = category.getBooks().stream()
	                    .map(book -> {
	                        BookDTO dto = new BookDTO();
	                        dto.setId(book.getId());
	                        dto.setTitle(book.getTitle());
	                        dto.setPrice(book.getPrice());
	                        dto.setQuantity(book.getQuantity());
	                        dto.setImageUrl(book.getImageUrl());
	                        return dto;
	                    })
	                    .collect(Collectors.toList());
	            
	            // Tạo DTO trả về
	            CategoryDetailDTO dto = new CategoryDetailDTO();
	            dto.setId(category.getId());
	            dto.setName(category.getName());
	            dto.setBooks(bookDTOs);
	            dto.setBookCount((long) bookDTOs.size()); // số lượng sách
	            return dto;
	        }).collect(Collectors.toList());
	        
	        return ResponseEntity.ok(dtos);
	    }
	    
	   

	    // ================== GET BY ID (chi tiết có sách) ==================
	    @GetMapping("/{id}")
	    public ResponseEntity<?> viewCategory(@PathVariable Integer id) {
	        Category category = em.find(Category.class, id);
	        if (category == null) {
	            return ResponseEntity.notFound().build();
	        }
	        List<Book> books = em.createQuery("SELECT b FROM Book b WHERE b.category.id = :cid", Book.class)
	                .setParameter("cid", id).getResultList();
	        CategoryDetailDTO dto = new CategoryDetailDTO();
	        dto.setId(category.getId());
	        dto.setName(category.getName());
	        List<BookDTO> bookDTOs = books.stream().map(b -> {
	            BookDTO bd = new BookDTO();
	            bd.setId(b.getId());
	            bd.setTitle(b.getTitle());
	            bd.setPrice(b.getPrice());
	            bd.setQuantity(b.getQuantity());
	            bd.setImageUrl(b.getImageUrl());
	            return bd;
	        }).collect(Collectors.toList());
	        dto.setBooks(bookDTOs);
	        return ResponseEntity.ok(dto);
	    }

	    // ================== GET BY ID (chỉ category) ==================
	    @GetMapping("/{id}/books")
	    public ResponseEntity<CategoryDTO> getOne(@PathVariable Integer id) {
	        return catRepo.findById(id)
	                .map(cat -> ResponseEntity.ok(new CategoryDTO(cat.getId(), cat.getName())))
	                .orElse(ResponseEntity.notFound().build());
	    }

	    // ================== CREATE ==================
	    @PostMapping
	    public ResponseEntity<?> create(@Valid @RequestBody CategoryDTO dto, BindingResult result) {
	        if (result.hasErrors()) {
	            return ResponseEntity.badRequest().body(result.getAllErrors());
	        }
	        Category category = new Category();
	        category.setName(dto.getName());
	        Category saved = catRepo.save(category);
	        return ResponseEntity.ok(new CategoryDTO(saved.getId(), saved.getName()));
	    }

	    // ================== UPDATE ==================
	    @PutMapping("/{id}")
	    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody CategoryDTO dto, BindingResult result) {
	        if (result.hasErrors()) {
	            return ResponseEntity.badRequest().body(result.getAllErrors());
	        }
	        return catRepo.findById(id).map(old -> {
	            old.setName(dto.getName());
	            Category updated = catRepo.save(old);
	            return ResponseEntity.ok(new CategoryDTO(updated.getId(), updated.getName()));
	        }).orElse(ResponseEntity.notFound().build());
	    }

	    // ================== DELETE ==================
	    @DeleteMapping("/{id}")
	    public ResponseEntity<?> delete(@PathVariable Integer id) {
	        try {
	            catRepo.deleteById(id);
	            return ResponseEntity.ok("Xóa thành công!");
	        } catch (Exception e) {
	            return ResponseEntity.badRequest().body("Không thể xóa thể loại đang có sách!");
	        }
	    }

}