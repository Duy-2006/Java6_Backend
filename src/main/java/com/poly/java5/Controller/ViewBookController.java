package com.poly.java5.Controller;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Service.BookService;
import com.poly.java5.Entity.Book;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
public class ViewBookController {
	 private final BookService bookService;

	    @GetMapping("/{id}")
	    public ResponseEntity<?> bookDetail(@PathVariable Integer id) {

	        try {
	            Book book = bookService.getBookById(id);
	            return ResponseEntity.ok(book);

	        } catch (Exception e) {
	            return ResponseEntity.status(404).body("Không tìm thấy sách");
	        }
	    }
}
