package com.poly.java5.Controller;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.poly.java5.DTO.BookDTO;
import com.poly.java5.DTO.ReviewRequestDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Repository.AuthorRepository;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.poly.java5.DTO.ReviewResponseDTO;
import com.poly.java5.Service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class BookApiController {

	private final BookRepository bookRepo;
	private final CategoryRepository catRepo;
	private final AuthorRepository authorRepo;
	private final ReviewService reviewService;

	@GetMapping("/{bookId}/reviews")
	public ResponseEntity<List<ReviewResponseDTO>> getReviews(@PathVariable Integer bookId) {
		return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId));
	}

	@PostMapping("/{bookId}/reviews")
	public ResponseEntity<ReviewResponseDTO> addReview(@PathVariable Integer bookId,
			@Valid @RequestBody ReviewRequestDTO request) {
		ReviewResponseDTO response = reviewService.addReview(bookId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// ==================== STOCK ====================
	@GetMapping("/{id}/stock")
	public ResponseEntity<Map<String, Integer>> getStock(@PathVariable Integer id) {
		Book book = bookRepo.findById(id).orElse(null);
		if (book == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(Map.of("stock", book.getQuantity()));
	}

	private BookDTO convertToDTO(Book b) {
		BookDTO dto = new BookDTO();
		dto.setId(b.getId());
		dto.setTitle(b.getTitle());
		dto.setPrice(b.getPrice());
		dto.setQuantity(b.getQuantity());
		dto.setImageUrl(b.getImageUrl());
		dto.setCategoryName(b.getCategory() != null ? b.getCategory().getName() : null);
		dto.setAuthorName(b.getAuthor() != null ? b.getAuthor().getName() : null);
		return dto;
	}

	// Lấy tất cả sách (phân trang)
	@GetMapping
	public Page<BookDTO> getBooks(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return bookRepo.findByDeletedFalse(pageable).map(this::convertToDTO);
	}

	@GetMapping("/{id}")
	public BookDTO getBook(@PathVariable Integer id) {
		Book b = bookRepo.findById(id).orElseThrow(() -> new RuntimeException("Sách không tồn tại"));
		return convertToDTO(b);
	}

	@GetMapping("/form-data")
	public Map<String, Object> getFormData() {
		return Map.of("categories", catRepo.findAll(), "authors", authorRepo.findAll());
	}

	@PostMapping
	public Book create(@RequestBody Book book) {
		return bookRepo.save(book);
	}

	@PutMapping("/{id}")
	public Book update(@PathVariable Integer id, @RequestBody Book book) {
		Book existing = bookRepo.findById(id).orElseThrow(() -> new RuntimeException("Book not found"));
		existing.setTitle(book.getTitle());
		existing.setIsbn(book.getIsbn());
		existing.setPrice(book.getPrice());
		existing.setPublisher(book.getPublisher());
		existing.setDescription(book.getDescription());
		existing.setActive(book.getActive());
		existing.setQuantity(book.getQuantity());
		existing.setCategory(book.getCategory());
		existing.setAuthor(book.getAuthor());
		return bookRepo.save(existing);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Integer id) {
		bookRepo.findById(id).ifPresent(book -> {
			book.setDeleted(true);
			bookRepo.save(book);
		});
	}

	// SÁCH MỚI - phân trang, mới nhất trước (dùng createdDate)
	@GetMapping("/new")
	public Page<BookDTO> newBooks(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
		return bookRepo.findByDeletedFalse(pageable).map(this::convertToDTO);
	}

	// SÁCH BÁN CHẠY TRONG TUẦN - dựa trên order details, giả sử lọc theo tuần hiện
	@GetMapping("/best-sellers")
	public Page<BookDTO> bestSellers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		// Lấy top sách bán chạy dựa trên tổng số lượng bán
		Page<Object[]> result = bookRepo.findTopSellingBooks(pageable);
		// Chuyển đổi thành Page<BookDTO>
		return result.map(obj -> convertToDTO((Book) obj[0]));
	}

}