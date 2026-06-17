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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

	@PersistenceContext
	private EntityManager em;

	@Autowired
	private CategoryRepository catRepo;

	// Đường dẫn lưu ảnh: thư mục static trong resources (giống như sách)
	private static final String UPLOAD_DIR = "src/main/resources/static/uploads/categories/";

	// Helper lưu file và trả về tên file
	private String saveImage(MultipartFile file) throws IOException {
		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}
		String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
		Path filePath = uploadPath.resolve(filename);
		Files.copy(file.getInputStream(), filePath);
		return filename;
	}

	// GET ALL
	@GetMapping
	public ResponseEntity<List<CategoryDetailDTO>> getAll() {
		List<Category> categories = catRepo.findAllWithBooks();
		List<CategoryDetailDTO> dtos = categories.stream().map(category -> {
			List<BookDTO> bookDTOs = category.getBooks().stream().map(book -> {
				BookDTO dto = new BookDTO();
				dto.setId(book.getId());
				dto.setTitle(book.getTitle());
				dto.setPrice(book.getPrice());
				dto.setQuantity(book.getQuantity());
				dto.setImageUrl(book.getImageUrl());
				return dto;
			}).collect(Collectors.toList());
			CategoryDetailDTO dto = new CategoryDetailDTO();
			dto.setId(category.getId());
			dto.setName(category.getName());
			dto.setImageUrl(category.getImageUrl());
			dto.setBooks(bookDTOs);
			dto.setBookCount((long) bookDTOs.size());
			return dto;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	// GET BY ID (chi tiết có sách)
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
		dto.setImageUrl(category.getImageUrl());
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

	// GET BY ID (chỉ category)
	@GetMapping("/{id}/books")
	public ResponseEntity<CategoryDTO> getOne(@PathVariable Integer id) {
		return catRepo.findById(id)
				.map(cat -> ResponseEntity.ok(new CategoryDTO(cat.getId(), cat.getName(), cat.getImageUrl())))
				.orElse(ResponseEntity.notFound().build());
	}

	// CREATE với upload ảnh
	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<?> create(@RequestParam("name") String name,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		Category category = new Category();
		category.setName(name);
		if (image != null && !image.isEmpty()) {
			try {
				String filename = saveImage(image);
				category.setImageUrl("/uploads/categories/" + filename);
			} catch (IOException e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi lưu ảnh: " + e.getMessage());
			}
		}
		Category saved = catRepo.save(category);
		CategoryDTO response = new CategoryDTO(saved.getId(), saved.getName(), saved.getImageUrl());
		return ResponseEntity.ok(response);
	}

	// UPDATE với upload ảnh
	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public ResponseEntity<?> update(@PathVariable Integer id, @RequestParam("name") String name,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		return catRepo.findById(id).map(old -> {
			old.setName(name);
			if (image != null && !image.isEmpty()) {
				try {
					String filename = saveImage(image);
					old.setImageUrl("/uploads/categories/" + filename);
				} catch (IOException e) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body("Lỗi lưu ảnh: " + e.getMessage());
				}
			}
			Category updated = catRepo.save(old);
			CategoryDTO response = new CategoryDTO(updated.getId(), updated.getName(), updated.getImageUrl());
			return ResponseEntity.ok(response);
		}).orElse(ResponseEntity.notFound().build());
	}

	// DELETE
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