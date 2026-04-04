package com.poly.java5.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.poly.java5.Entity.Promotion;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Repository.PromotionRepository;
import com.poly.java5.Service.PromotionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/promotions")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminPromotionApiController {

    private final PromotionService promotionService;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    // ================= LIST =================
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
            java.util.Map.of(
                "promotions", promotionService.getAll(),
                "today", LocalDate.now()
            )
        );
    }

    // ================= FORM DATA =================
    // 👉 dùng cho Vue load dropdown
    @GetMapping("/form-data")
    public ResponseEntity<?> getFormData() {
        return ResponseEntity.ok(
            java.util.Map.of(
                "books", bookRepository.findByDeletedFalse(),
                "categories", categoryRepository.findAll()
            )
        );
    }

    // ================= CREATE =================
    @PostMapping
    public ResponseEntity<?> createPromotion(
            @RequestBody Promotion promotion,
            @RequestParam(required = false) List<Integer> bookIds,
            @RequestParam(required = false) List<Integer> categoryIds) {

        try {
            promotionService.createPromotion(promotion, bookIds, categoryIds);
            return ResponseEntity.ok("Tạo khuyến mãi thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // ================= DETAIL =================
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {

        return promotionService.findById(id)
                .map(p -> ResponseEntity.ok(p))
                .orElse(ResponseEntity.notFound().build());
    }

    // ================= UPDATE =================
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePromotion(
            @PathVariable Integer id,
            @RequestBody Promotion promotion,
            @RequestParam(required = false) List<Integer> bookIds,
            @RequestParam(required = false) List<Integer> categoryIds) {

        try {
            promotion.setId(id); // đảm bảo đúng ID
            promotionService.updatePromotion(promotion, bookIds, categoryIds);
            return ResponseEntity.ok("Cập nhật thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // ================= DELETE =================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePromotion(@PathVariable Integer id) {

        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.ok("Xóa thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Xóa thất bại: " + e.getMessage());
        }
    }
}