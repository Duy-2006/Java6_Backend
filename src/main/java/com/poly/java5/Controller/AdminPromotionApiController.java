package com.poly.java5.Controller;

import java.util.List;
import java.util.Map;

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

import com.poly.java5.DTO.PromotionDTO;
import com.poly.java5.Entity.Promotion;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Service.PromotionService;

import lombok.RequiredArgsConstructor;

// ✅ Bỏ wildcard "*" — phải chỉ rõ origin khi FE gửi Authorization header
@RestController
@RequestMapping("/api/admin/promotions")

@RequiredArgsConstructor
public class AdminPromotionApiController {

	  private final PromotionService   promotionService;
	    private final BookRepository     bookRepository;
	    private final CategoryRepository categoryRepository;

	    // ─────────────────── Helper: Entity → DTO ──────────────────────────────
	    private PromotionDTO toDTO(Promotion p) {
	        PromotionDTO dto = new PromotionDTO();
	        dto.setId(p.getId());
	        dto.setName(p.getName());
	        dto.setDiscountValue(p.getDiscountValue());
	        dto.setStartDate(p.getStartDate());
	        dto.setEndDate(p.getEndDate());
	        dto.setStatus(p.getStatus());
	        dto.setUsageLimit(p.getUsageLimit());
	        dto.setApplyType(p.getApplyType());
	        dto.setComputedStatus(p.getComputedStatus()); // @Transient

	        // Lấy books / categories từ PromotionDetail
	        if (p.getDetails() != null) {
	            dto.setBookIds(
	                p.getDetails().stream()
	                    .filter(d -> d.getBook() != null)
	                    .map(d -> d.getBook().getId())
	                    .toList()
	            );
	            dto.setBookTitles(
	                p.getDetails().stream()
	                    .filter(d -> d.getBook() != null)
	                    .map(d -> d.getBook().getTitle())
	                    .toList()
	            );
	            dto.setCategoryIds(
	                p.getDetails().stream()
	                    .filter(d -> d.getCategory() != null)
	                    .map(d -> d.getCategory().getId())
	                    .toList()
	            );
	            dto.setCategoryNames(
	                p.getDetails().stream()
	                    .filter(d -> d.getCategory() != null)
	                    .map(d -> d.getCategory().getName())
	                    .toList()
	            );
	        }

	        return dto;
	    }

	    // ─────────────────── Helper: DTO → Entity ──────────────────────────────
	    private Promotion toEntity(PromotionDTO dto) {
	        Promotion p = new Promotion();
	        p.setName(dto.getName());
	        p.setDiscountValue(dto.getDiscountValue());
	        p.setStartDate(dto.getStartDate());
	        p.setEndDate(dto.getEndDate());
	        p.setStatus(dto.getStatus() != null ? dto.getStatus() : true);
	        p.setUsageLimit(dto.getUsageLimit());
	        p.setApplyType(dto.getApplyType());
	        return p;
	    }

	    // ================= LIST =================
	    @GetMapping
	    @org.springframework.transaction.annotation.Transactional(readOnly = true)
	    public List<PromotionDTO> getAll() {
	        return promotionService.getAll().stream()
	                .map(this::toDTO)
	                .toList();
	    }

	    // ================= DETAIL =================
	    @GetMapping("/{id}")
	    @org.springframework.transaction.annotation.Transactional(readOnly = true)
	    public ResponseEntity<PromotionDTO> getById(@PathVariable Integer id) {
	        return promotionService.findById(id)
	                .map(p -> ResponseEntity.ok(toDTO(p)))
	                .orElse(ResponseEntity.notFound().build());
	    }

	    // ================= FORM DATA =================
	    @GetMapping("/form-data")
	    @org.springframework.transaction.annotation.Transactional(readOnly = true)
	    public ResponseEntity<?> getFormData() {
	        var books = bookRepository.findByDeletedFalse().stream()
	            .map(b -> Map.of("id", b.getId(), "title", b.getTitle()))
	            .toList();
	            
	        var categories = categoryRepository.findAll().stream()
	            .map(c -> Map.of("id", c.getId(), "name", c.getName()))
	            .toList();

	        return ResponseEntity.ok(
	            Map.of(
	                "books", books,
	                "categories", categories
	            )
	        );
	    }

	    // ================= CREATE =================
	    @PostMapping
	    public ResponseEntity<?> createPromotion(@RequestBody PromotionDTO dto) {
	        try {
	            Promotion promotion = toEntity(dto);
	            promotionService.createPromotion(promotion, dto.getBookIds(), dto.getCategoryIds());
	            return ResponseEntity.ok("Tạo khuyến mãi thành công");
	        } catch (Exception e) {
	            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
	        }
	    }

	    // ================= UPDATE =================
	    @PutMapping("/{id}")
	    public ResponseEntity<?> updatePromotion(
	            @PathVariable Integer id,
	            @RequestBody PromotionDTO dto) {
	        try {
	            Promotion promotion = toEntity(dto);
	            promotion.setId(id);
	            promotionService.updatePromotion(promotion, dto.getBookIds(), dto.getCategoryIds());
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