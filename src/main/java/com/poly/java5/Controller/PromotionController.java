package com.poly.java5.Controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.Service.PromotionService;

import lombok.RequiredArgsConstructor;
import com.poly.java5.DTO.PromotionDTO;
import com.poly.java5.Entity.Promotion;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;

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
        dto.setComputedStatus(p.getComputedStatus());

        if (p.getDetails() != null) {
            dto.setBookIds(p.getDetails().stream().filter(d -> d.getBook() != null).map(d -> d.getBook().getId()).toList());
            dto.setBookTitles(p.getDetails().stream().filter(d -> d.getBook() != null).map(d -> d.getBook().getTitle()).toList());
            dto.setCategoryIds(p.getDetails().stream().filter(d -> d.getCategory() != null).map(d -> d.getCategory().getId()).toList());
            dto.setCategoryNames(p.getDetails().stream().filter(d -> d.getCategory() != null).map(d -> d.getCategory().getName()).toList());
        }
        return dto;
    }

    // Lấy danh sách khuyến mãi đang active
    @GetMapping("/active")
    public ResponseEntity<List<PromotionDTO>> getActivePromotions() {
        List<PromotionDTO> activePromos = promotionService.getAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()) && "ACTIVE".equals(p.getComputedStatus()))
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(activePromos);
    }

    // lấy giá cuối cùng của 1 cuốn sách
    @GetMapping("/final-price/{bookId}")
    public ResponseEntity<BigDecimal> getFinalPrice(@PathVariable Integer bookId) {

        BigDecimal finalPrice = promotionService.getFinalPrice(bookId);

        return ResponseEntity.ok(finalPrice);
    }
}
