package com.poly.java5.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.Entity.Book;
import com.poly.java5.Service.BookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
	  private final BookService bookService;
	    
	    // 📖 LẤY TẤT CẢ SÁCH
	    @GetMapping("")
	    public ResponseEntity<Map<String, Object>> getAllBooks() {
	        Map<String, Object> response = new HashMap<>();
	        
	        try {
	            List<Book> books = bookService.getAllBooks();
	            
	            response.put("success", true);
	            response.put("data", books);
	            response.put("message", "Lấy danh sách sách thành công");
	            response.put("count", books.size());
	            
	            return ResponseEntity.ok(response);
	            
	        } catch (Exception e) {
	            response.put("success", false);
	            response.put("message", "Lỗi: " + e.getMessage());
	            return ResponseEntity.badRequest().body(response);
	        }
	    }
	    
	    // 📖 LẤY SÁCH THEO ID
	    @GetMapping("/{id}")
	    public ResponseEntity<Map<String, Object>> getBookById(@PathVariable Integer id) {
	        Map<String, Object> response = new HashMap<>();
	        
	        try {
	            Book book = bookService.getBookById(id);
	            
	            response.put("success", true);
	            response.put("data", book);
	            response.put("message", "Lấy thông tin sách thành công");
	            
	            return ResponseEntity.ok(response);
	            
	        } catch (Exception e) {
	            response.put("success", false);
	            response.put("message", e.getMessage());
	            return ResponseEntity.badRequest().body(response);
	        }
	    }
	    
	    // 🔍 TÌM KIẾM SÁCH
	    @GetMapping("/search")
	    public ResponseEntity<Map<String, Object>> searchBooks(
	            @RequestParam(required = false) String title,
	            @RequestParam(required = false) String author,
	            @RequestParam(required = false) String category,
	            @RequestParam(required = false) String isbn) {

	        Map<String, Object> response = new HashMap<>();

	        try {
	            List<Book> books = bookService.searchBooks(
	                    title
	            );

	            response.put("success", true);
	            response.put("data", books);
	            response.put("count", books.size());
	            response.put("message", "Kết quả tìm kiếm sách");

	            return ResponseEntity.ok(response);

	        } catch (Exception e) {
	            response.put("success", false);
	            response.put("message", e.getMessage());
	            return ResponseEntity.badRequest().body(response);
	        }
	    }
	    
	    // 🆕 LẤY SÁCH MỚI NHẤT
	    @GetMapping("/new")
	    public ResponseEntity<Map<String, Object>> getNewBooks() {
	        Map<String, Object> response = new HashMap<>();
	        
	        try {
	            List<Book> books = bookService.getNewBooks();
	            
	            response.put("success", true);
	            response.put("data", books);
	            response.put("message", "10 sách mới nhất");
	            response.put("count", books.size());
	            
	            return ResponseEntity.ok(response);
	            
	        } catch (Exception e) {
	            response.put("success", false);
	            response.put("message", "Lỗi: " + e.getMessage());
	            return ResponseEntity.badRequest().body(response);
	        }
	    }
}
