package com.poly.java5.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.AuthorDTO;
import com.poly.java5.Entity.Author;
import com.poly.java5.Repository.AuthorRepository;

@RestController
@RequestMapping("/api/authors")
@CrossOrigin("*")
public class AuthorApiController {
	 @Autowired
	    AuthorRepository authorRepo;

	    // ================== GET ALL ==================
	    @GetMapping
	    public List<AuthorDTO> getAll() {
	        return authorRepo.findAll().stream().map(a -> {
	            AuthorDTO dto = new AuthorDTO();
	            dto.setId(a.getId());
	            dto.setName(a.getName());
	            return dto;
	        }).toList();
	    }

	    // ================== GET BY ID ==================
	    @GetMapping("/{id}")
	    public ResponseEntity<?> getOne(@PathVariable Long id) {
	        return authorRepo.findById(id)
	                .map(a -> {
	                    AuthorDTO dto = new AuthorDTO();
	                    dto.setId(a.getId());
	                    dto.setName(a.getName());
	                    return ResponseEntity.ok(dto);
	                })
	                .orElse(ResponseEntity.notFound().build());
	    }

	    // ================== CREATE ==================
	    @PostMapping
	    public ResponseEntity<?> create(@RequestBody AuthorDTO dto) {
	        Author author = new Author();
	        author.setName(dto.getName());
	        return ResponseEntity.ok(authorRepo.save(author));
	    }

	    // ================== UPDATE ==================
	    @PutMapping("/{id}")
	    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AuthorDTO dto) {
	        return authorRepo.findById(id)
	                .map(a -> {
	                    a.setName(dto.getName());
	                    return ResponseEntity.ok(authorRepo.save(a));
	                })
	                .orElse(ResponseEntity.notFound().build());
	    }

	    // ================== DELETE ==================
	    @DeleteMapping("/{id}")
	    public ResponseEntity<?> delete(@PathVariable Long id) {
	        try {
	            authorRepo.deleteById(id);
	            return ResponseEntity.ok("Xóa thành công!");
	        } catch (Exception e) {
	            return ResponseEntity.badRequest().body("Không thể xóa tác giả đang có sách!");
	        }
	    }
}
