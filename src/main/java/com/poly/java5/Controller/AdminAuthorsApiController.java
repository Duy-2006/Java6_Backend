package com.poly.java5.Controller;


import com.poly.java5.DTO.AuthorDTO;
import com.poly.java5.Entity.Author;
import com.poly.java5.Entity.Category;
import com.poly.java5.Service.AuthorService;
import com.poly.java5.Service.CategoryService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────────────────────
//  AUTHORS  /api/admin/authors
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/admin/authors")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
class AdminAuthorsApiController {

	@Autowired
    private AuthorService authorService;

    // GET ALL - trả về DTO
	@GetMapping
	public ResponseEntity<List<AuthorDTO>> getAll() {
	    List<AuthorDTO> dtos = authorService.findAllWithBookCount(); // thay đổi ở đây
	    return ResponseEntity.ok(dtos);
	}
    // GET ONE - trả về DTO
	@GetMapping("/{id}")
	public ResponseEntity<AuthorDTO> getOne(@PathVariable Long id) {
	    AuthorDTO dto = authorService.findByIdWithBookCount(id);
	    if (dto == null) return ResponseEntity.notFound().build();
	    return ResponseEntity.ok(dto);
	}

    // CREATE - nhận DTO, trả về DTO
    @PostMapping
public ResponseEntity<?> create(@Valid @RequestBody AuthorDTO dto, BindingResult result) {
    if (result.hasErrors()) {
        return ResponseEntity.badRequest().body(result.getAllErrors());
    }
    Author author = new Author();
    author.setName(dto.getName());
    author.setEmail(dto.getEmail());
    Author saved = authorService.save(author);
    // Trả về DTO với bookCount = 0
    return ResponseEntity.ok(new AuthorDTO(saved.getId(), saved.getName(), saved.getEmail(), 0L));
}

    // UPDATE - nhận DTO, trả về DTO
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody AuthorDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }
        Author author = authorService.findById(id);
        if (author == null) return ResponseEntity.notFound().build();
        author.setName(dto.getName());
        author.setEmail(dto.getEmail());
        Author updated = authorService.save(author);
        
        // Lấy lại bookCount hiện tại (có thể tính lại hoặc lấy từ DTO cũ)
        Long currentBookCount = authorService.getBookCountByAuthorId(id); // cần viết method này
        return ResponseEntity.ok(new AuthorDTO(updated.getId(), updated.getName(), updated.getEmail(), currentBookCount));
    }

    // DELETE - giữ nguyên
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        authorService.deleteById(id);
        return ResponseEntity.ok().body("Xóa thành công");
    }
}


