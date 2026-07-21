package com.poly.java5.ai.controller;

import com.poly.java5.ai.service.BookIndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.poly.java5.ai.tool.BookstoreTools;

@RestController
@RequestMapping("/api/admin/ai-index")
public class ChatbotAdminController {

    private final BookIndexingService bookIndexingService;
    private final BookstoreTools bookstoreTools;

    public ChatbotAdminController(BookIndexingService bookIndexingService, BookstoreTools bookstoreTools) {
        this.bookIndexingService = bookIndexingService;
        this.bookstoreTools = bookstoreTools;
    }

    @PostMapping("/rebuild")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rebuildIndex() {
        bookIndexingService.indexAllBooks();
        return ResponseEntity.ok(Map.of("message", "Đã lập chỉ mục toàn bộ sách thành công"));
    }

    @PostMapping("/books/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> indexBook(@PathVariable Integer bookId) {
        bookIndexingService.indexBook(bookId);
        return ResponseEntity.ok(Map.of("message", "Đã cập nhật chỉ mục sách ID " + bookId));
    }

    @DeleteMapping("/books/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBookIndex(@PathVariable Integer bookId) {
        bookIndexingService.removeBookFromIndex(bookId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa chỉ mục sách ID " + bookId));
    }
}
