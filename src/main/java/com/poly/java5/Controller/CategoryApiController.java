package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
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

@RestController
@RequestMapping("/api/categories")
@CrossOrigin("*") // cho Vue gọi
public class CategoryApiController {
	@PersistenceContext
	private EntityManager em;

	@GetMapping("/{id}")
	public ResponseEntity<?> viewCategory(@PathVariable Integer id) {

		Category category = em.find(Category.class, id);

		if (category == null) {
			return ResponseEntity.notFound().build();
		}

		List<Book> books = em.createQuery("SELECT b FROM Book b WHERE b.category.id = :cid", Book.class)
				.setParameter("cid", id).getResultList();

		// 👉 map sang DTO
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
		}).toList();

		dto.setBooks(bookDTOs);

		return ResponseEntity.ok(dto);
	}

	@Autowired
	CategoryRepository catRepo;

	// ================== GET ALL ==================
	@GetMapping
	public List<Category> getAll() {
		return catRepo.findAll();
	}

	// ================== GET BY ID ==================
	@GetMapping("/{id}/books")
	public ResponseEntity<?> getOne(@PathVariable Integer id) {
		return catRepo.findById(id).map(cat -> ResponseEntity.ok(cat)).orElse(ResponseEntity.notFound().build());
	}

	// ================== CREATE ==================
	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody Category category, BindingResult result) {
		if (result.hasErrors()) {
			return ResponseEntity.badRequest().body(result.getAllErrors());
		}
		return ResponseEntity.ok(catRepo.save(category));
	}

	// ================== UPDATE ==================
	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody Category category,
			BindingResult result) {
		if (result.hasErrors()) {
			return ResponseEntity.badRequest().body(result.getAllErrors());
		}

		return catRepo.findById(id).map(old -> {
			old.setName(category.getName()); // sửa theo field của bạn
			return ResponseEntity.ok(catRepo.save(old));
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
