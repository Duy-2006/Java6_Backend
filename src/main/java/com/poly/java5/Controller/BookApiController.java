package com.poly.java5.Controller;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Repository.AuthorRepository;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class BookApiController {
	 private final BookRepository bookRepo;
	    private final CategoryRepository catRepo;
	    private final AuthorRepository authorRepo;

	    // lấy danh sách sách 
	    @GetMapping
	    public List<BookDTO> getBooks() {
	        return bookRepo.findByDeletedFalse().stream().map(b -> {
	            BookDTO dto = new BookDTO();
	            dto.setId(b.getId());
	            dto.setTitle(b.getTitle());
	            dto.setPrice(b.getPrice());
	            dto.setQuantity(b.getQuantity());
	            dto.setImageUrl(b.getImageUrl());
	            dto.setCategoryName(
	                b.getCategory() != null ? b.getCategory().getName() : null
	            );
	            dto.setAuthorName(
	                b.getAuthor() != null ? b.getAuthor().getName() : null
	            );
	            return dto;
	        }).toList();
	    }

	    // lấy chi tiết 1 sách 
	    @GetMapping("/{id}")
	    public BookDTO getBook(@PathVariable Integer id) {

	        Book b = bookRepo.findById(id)
	                .orElseThrow(() -> new RuntimeException("sách không có "));

	        BookDTO dto = new BookDTO();
	        dto.setId(b.getId());
	        dto.setTitle(b.getTitle());
	        dto.setPrice(b.getPrice());
	        dto.setQuantity(b.getQuantity());
	        dto.setImageUrl(b.getImageUrl());
	        dto.setCategoryName(
	            b.getCategory() != null ? b.getCategory().getName() : null
	        );
	        dto.setAuthorName(
	            b.getAuthor() != null ? b.getAuthor().getName() : null
	        );

	        return dto;
	    }

	    // lấy dữ liệu từ  fom danh mục và tác giả 
	    @GetMapping("/form-data")
	    public Map<String, Object> getFormData() {
	        return Map.of(
	            "categories", catRepo.findAll(),
	            "authors", authorRepo.findAll()
	        );
	    }

	    // ================= CREATE =================
	    @PostMapping
	    public Book create(@RequestBody Book book) {
	        return bookRepo.save(book);
	    }

	    // ================= UPDATE =================
	    @PutMapping("/{id}")
	    public Book update(@PathVariable Integer id, @RequestBody Book book) {

	        Book existing = bookRepo.findById(id)
	                .orElseThrow(() -> new RuntimeException("Book not found"));

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

	    // ================= DELETE =================
	    @DeleteMapping("/{id}")
	    public void delete(@PathVariable Integer id) {
	        bookRepo.findById(id).ifPresent(book -> {
	            book.setDeleted(true);
	            bookRepo.save(book);
	        });
	    }

	    // ================= NEW =================
	    @GetMapping("/new")
	    public List<BookDTO> newBooks() {
	        Pageable pageable = PageRequest.of(0, 10);
	        return bookRepo.findTop10ActiveNewBooks(pageable).stream()
	        		.map(b -> {
	        	        BookDTO dto = new BookDTO();
	        	        dto.setId(b.getId());
	        	        dto.setTitle(b.getTitle());
	        	        dto.setPrice(b.getPrice());
	        	        dto.setQuantity(b.getQuantity());
	        	        dto.setImageUrl(b.getImageUrl());
	        	        dto.setCategoryName(b.getCategory() != null ? b.getCategory().getName() : null);
	        	        dto.setAuthorName(b.getAuthor() != null ? b.getAuthor().getName() : null);
	        	        return dto;
	        	    }).toList();
	    }
}
