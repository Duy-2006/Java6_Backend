package com.poly.java5.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Service.BookService;
import com.poly.java5.Entity.Book;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
public class ViewBookController {

    private final BookService bookService;

    // GET DETAIL BOOK
    @GetMapping("/{id}")
    public ResponseEntity<?> bookDetail(@PathVariable Integer id) {

        Book book = bookService.findById(id);

        if (book == null) {
            return ResponseEntity.status(404).body("Không tìm thấy sách");
        }

        return ResponseEntity.ok(book);
    }
}