package com.poly.java5.Controller;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Service.ImageSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class ImageSearchApiController {

    private final ImageSearchService imageSearchService;

    /**
     * REST API Tìm kiếm sách bằng hình ảnh cho Khách hàng & User.
     * Endpoint: POST /api/books/search-by-image
     */
    @PostMapping("/api/books/search-by-image")
    public ResponseEntity<List<BookDTO>> searchByImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "topK", defaultValue = "10") int topK) {
        
        log.info("Nhận request tìm kiếm bằng hình ảnh: filename={}, size={} bytes, topK={}",
                image.getOriginalFilename(), image.getSize(), topK);

        List<BookDTO> results = imageSearchService.searchByImage(image, topK);
        return ResponseEntity.ok(results);
    }

    /**
     * REST API cho Admin Re-index lại toàn bộ sách cho tính năng Image Search.
     * Endpoint: POST /api/admin/books/reindex-images
     */
    @PostMapping("/api/admin/books/reindex-images")
    public ResponseEntity<Map<String, Object>> reindexAllImages() {
        log.info("Admin yêu cầu Re-index toàn bộ ảnh bìa sách cho FAISS Image Search...");
        Map<String, Object> response = imageSearchService.reindexAllBooks();
        return ResponseEntity.ok(response);
    }
}
