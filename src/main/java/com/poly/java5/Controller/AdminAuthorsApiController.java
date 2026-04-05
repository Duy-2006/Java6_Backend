package com.poly.java5.Controller;


import com.poly.java5.Entity.Author;
import com.poly.java5.Entity.Category;
import com.poly.java5.Service.AuthorService;
import com.poly.java5.Service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// ─────────────────────────────────────────────────────────────
//  AUTHORS  /api/admin/authors
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/admin/authors")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
class AdminAuthorsApiController {

    @Autowired private AuthorService authorService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(authorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        Author a = authorService.findById(id);
        if (a == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(a);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Author a = new Author();
        a.setName(body.get("name"));
        a.setEmail(body.getOrDefault("email", null));
        authorService.save(a);
        return ResponseEntity.ok(Map.of("message", "Thêm tác giả thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, String> body) {
        Author a = authorService.findById(id);
        if (a == null) return ResponseEntity.notFound().build();
        if (body.containsKey("name"))  a.setName(body.get("name"));
        if (body.containsKey("email")) a.setEmail(body.get("email"));
        authorService.save(a);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        authorService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa tác giả"));
    }
}

// ─────────────────────────────────────────────────────────────
//  CATEGORIES  /api/admin/categories
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
class AdminCategoriesApiController {

    @Autowired private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Integer id) {
        Category c = categoryService.findById(id);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(c);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Category c = new Category();
        c.setName(body.get("name"));
        categoryService.save(c);
        return ResponseEntity.ok(Map.of("message", "Thêm thể loại thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id,
                                    @RequestBody Map<String, String> body) {
        Category c = categoryService.findById(id);
        if (c == null) return ResponseEntity.notFound().build();
        c.setName(body.get("name"));
        categoryService.save(c);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        categoryService.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thể loại"));
    }
}