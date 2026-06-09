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
    private final com.poly.java5.Repository.BookFormatRepository bookFormatRepo;

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
        dto.setActive(b.getActive()); // Thêm active để frontend có thể dùng nếu cần
        
        Long sold = bookRepo.getSoldCountById(b.getId());
        dto.setSoldCount(sold);
        
        // Lấy giá sách nói
        bookFormatRepo.findByBookIdAndFormatType(b.getId(), "AUDIO")
                .ifPresent(f -> dto.setAudioPrice(f.getPrice()));
        
        return dto;
    }

    // Lấy tất cả sách (phân trang) - chỉ lấy active = true
    @GetMapping
    public Page<BookDTO> getBooks(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepo.findByActiveTrue(pageable).map(this::convertToDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBook(@PathVariable Integer id) {
        // Chỉ trả về nếu sách tồn tại và active = true
        Book b = bookRepo.findByIdAndActiveTrue(id).orElse(null);
        if (b == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToDTO(b));
    }

    @GetMapping("/form-data")
    public Map<String, Object> getFormData() {
        return Map.of("categories", catRepo.findAll(), "authors", authorRepo.findAll());
    }

    // ========== CÁC ENDPOINT CHO ADMIN (HOẶC CHỈ DÙNG NỘI BỘ) ==========
    // Lưu ý: Các endpoint POST, PUT, DELETE nên để riêng cho admin, nhưng giữ lại cho tương thích
    @PostMapping
    public Book create(@RequestBody Book book) {
        // Không nên cho phép tạo mới qua API này nếu không kiểm tra quyền
        return bookRepo.save(book);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable Integer id, @RequestBody Book book) {
        // Không nên cho phép update qua API này
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
        // Xóa mềm - nhưng nên chuyển sang admin controller
        bookRepo.findById(id).ifPresent(book -> {
            book.setDeleted(true);
            bookRepo.save(book);
        });
    }

    // SÁCH MỚI - chỉ lấy active = true, mới nhất trước
    @GetMapping("/new")
    public Page<BookDTO> newBooks(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        // Sửa thành findByActiveTrue (hoặc method riêng có sort)
        return bookRepo.findByActiveTrue(pageable).map(this::convertToDTO);
    }

    // SÁCH BÁN CHẠY - chỉ lấy active = true
    @GetMapping("/best-sellers")
    public Page<BookDTO> bestSellers(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> result = bookRepo.findTopSellingBooksActiveOnly(pageable);
        return result.map(obj -> {
            BookDTO dto = convertToDTO((Book) obj[0]);
            // Ghi đè soldCount từ query nếu cần thiết, obj[1] là Long
            if (obj[1] != null) {
                dto.setSoldCount(((Number) obj[1]).longValue());
            }
            return dto;
        });
    }

    // SÁCH NÓI - chỉ lấy active = true và có định dạng AUDIO active
    @GetMapping("/audiobooks")
    public Page<BookDTO> audiobooks(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepo.findAudiobooksActiveOnly(pageable).map(this::convertToDTO);
    }
}