package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Service.BookService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/books")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminBooksApiController {

	@Autowired
	private BookService bookService;

	private static final String UPLOAD_DIR = "src/main/resources/static/uploads/books/";

	// ================= GET ALL =================
	@GetMapping
	public ResponseEntity<List<BookDTO>> getAll() {
		List<BookDTO> dtos = bookService.getAllBooks().stream().map(this::convertToDTO).collect(Collectors.toList());

		return ResponseEntity.ok(dtos);
	}

	// ================= GET ONE =================
	@GetMapping("/{id}")
	public ResponseEntity<?> getOne(@PathVariable Integer id) {
		Book book = bookService.findById(id);

		if (book == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}

		return ResponseEntity.ok(convertToDTO(book));
	}

	// ================= CREATE =================
	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<?> create(@Valid @ModelAttribute BookDTO dto, BindingResult result) throws Exception {

		if (result.hasErrors()) {
			return ResponseEntity.badRequest().body(result.getAllErrors());
		}

		Book book = convertToEntity(dto);

		// upload ảnh
		if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
			book.setImageUrl(saveImage(dto.getImageFile()));
		}

		bookService.save(book);

		return ResponseEntity.ok(convertToDTO(book));
	}

	// ================= UPDATE =================
	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public ResponseEntity<?> update(@PathVariable Integer id, @Valid @ModelAttribute BookDTO dto, BindingResult result)
			throws Exception {

		if (result.hasErrors()) {
			return ResponseEntity.badRequest().body(result.getAllErrors());
		}

		Book existing = bookService.findById(id);

		if (existing == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}

		// cập nhật dữ liệu
		existing.setTitle(dto.getTitle());
		existing.setIsbn(dto.getIsbn());
		existing.setPublisher(dto.getPublisher());
		existing.setPrice(dto.getPrice());
		existing.setQuantity(dto.getQuantity());
		existing.setActive(dto.getActive());
		existing.setDescription(dto.getDescription());

		// AUTHOR (Long)
		if (dto.getAuthorId() != null) {
			existing.setAuthor(bookService.findAuthorById(dto.getAuthorId()));
		}

		// CATEGORY (Integer)
		if (dto.getCategoryId() != null) {
			existing.setCategory(bookService.findCategoryById(dto.getCategoryId()));
		}

		// upload ảnh mới
		if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
			existing.setImageUrl(saveImage(dto.getImageFile()));
		}

		bookService.save(existing);

		return ResponseEntity.ok(convertToDTO(existing));
	}

	// ================= DELETE =================
	// DELETE (Ẩn sách)
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Integer id) {
		Book book = bookService.findById(id);
		if (book == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}
		book.setActive(false); // Ẩn: active = false
		bookService.save(book);
		return ResponseEntity.ok("Đã ẩn sách thành công");
	}

// RESTORE (Hiện sách)
	@PutMapping("/{id}/restore")
	public ResponseEntity<?> restore(@PathVariable Integer id) {
		Book book = bookService.findById(id);
		if (book == null) {
			return ResponseEntity.status(404).body("Không tìm thấy sách");
		}
		book.setActive(true); // Hiện: active = true
		bookService.save(book);
		return ResponseEntity.ok("Đã bật sách thành công");
	}

	// ================= CONVERT ENTITY -> DTO =================
	private BookDTO convertToDTO(Book book) {
		BookDTO dto = new BookDTO();

		dto.setId(book.getId()); // Integer
		dto.setTitle(book.getTitle());
		dto.setIsbn(book.getIsbn());
		dto.setPublisher(book.getPublisher());
		dto.setPrice(book.getPrice());
		dto.setQuantity(book.getQuantity());
		dto.setActive(book.getActive());
		dto.setDescription(book.getDescription());
		dto.setImageUrl(book.getImageUrl());

		// Author (Long)
		if (book.getAuthor() != null) {
			dto.setAuthorId(book.getAuthor().getId());
			dto.setAuthorName(book.getAuthor().getName());
		}

		// Category (Integer)
		if (book.getCategory() != null) {
			dto.setCategoryId(book.getCategory().getId());
			dto.setCategoryName(book.getCategory().getName());
		}

		return dto;
	}

	// ================= CONVERT DTO -> ENTITY =================
	private Book convertToEntity(BookDTO dto) {
		Book book = new Book();

		book.setTitle(dto.getTitle());
		book.setIsbn(dto.getIsbn());
		book.setPublisher(dto.getPublisher());
		book.setPrice(dto.getPrice());
		book.setQuantity(dto.getQuantity());
		book.setActive(dto.getActive());
		book.setDescription(dto.getDescription());

		// Author (Long)
		if (dto.getAuthorId() != null) {
			book.setAuthor(bookService.findAuthorById(dto.getAuthorId()));
		}

		// Category (Integer)
		if (dto.getCategoryId() != null) {
			book.setCategory(bookService.findCategoryById(dto.getCategoryId()));
		}

		// ID (Integer)
		if (dto.getId() != null) {
			book.setId(dto.getId());
		}

		return book;
	}

	// ================= SAVE IMAGE =================
	private String saveImage(MultipartFile file) throws Exception {
		String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

		Files.createDirectories(Paths.get(UPLOAD_DIR));

		Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR + fileName), StandardCopyOption.REPLACE_EXISTING);

		return fileName;
	}
}