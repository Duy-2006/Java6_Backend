package com.poly.java5.Controller;

import com.poly.java5.Entity.Book;
import com.poly.java5.Service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/books")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminBooksApiController {

    @Autowired
    private BookService bookService;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/books/";

    // GET /api/admin/books
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    // GET /api/admin/books/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {   // Giữ Long cũng được nếu Service hỗ trợ
        Book book = bookService.findById(id);
        if (book == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(book);
    }

    // POST tạo mới sách
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> create(
            @RequestParam String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) Long categoryId,
            @RequestParam double price,           // nhận double từ form
            @RequestParam int quantity,
            @RequestParam boolean active,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile imageFile) throws Exception {

        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublisher(publisher);
        book.setPrice(BigDecimal.valueOf(price));   // ← SỬA Ở ĐÂY
        book.setQuantity(quantity);
        book.setActive(active);
        book.setDescription(description);

        if (authorId != null) {
            book.setAuthor(bookService.findAuthorById(authorId));
        }
        if (categoryId != null) {
            book.setCategory(bookService.findCategoryById(categoryId));
        }

        // Upload ảnh
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Files.createDirectories(Paths.get(UPLOAD_DIR)); // đảm bảo thư mục tồn tại
            Files.copy(imageFile.getInputStream(),
                    Paths.get(UPLOAD_DIR + fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            book.setImageUrl(fileName);
        }

        bookService.save(book);
        return ResponseEntity.ok(Map.of("message", "Thêm sách thành công", "bookId", book.getId()));
    }

    // PUT cập nhật sách
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) Long categoryId,
            @RequestParam double price,
            @RequestParam int quantity,
            @RequestParam boolean active,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile imageFile) throws Exception {

        Book book = bookService.findById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublisher(publisher);
        book.setPrice(BigDecimal.valueOf(price));   // ← SỬA Ở ĐÂY
        book.setQuantity(quantity);
        book.setActive(active);
        book.setDescription(description);

        if (authorId != null) {
            book.setAuthor(bookService.findAuthorById(authorId));
        }
        if (categoryId != null) {
            book.setCategory(bookService.findCategoryById(categoryId));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            Files.copy(imageFile.getInputStream(),
                    Paths.get(UPLOAD_DIR + fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            book.setImageUrl(fileName);
        }

        bookService.save(book);
        return ResponseEntity.ok(Map.of("message", "Cập nhật sách thành công"));
    }

    // DELETE sách
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {   // ← SỬA: đổi thành Integer vì Book dùng Integer id
        bookService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sách thành công"));
    }
}