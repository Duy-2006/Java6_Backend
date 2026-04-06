package com.poly.java5.Controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.Service.PromotionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // lấy giá cuối cùng của 1 cuốn sách
    @GetMapping("/final-price/{bookId}")
    public ResponseEntity<BigDecimal> getFinalPrice(@PathVariable Integer bookId) {

        BigDecimal finalPrice = promotionService.getFinalPrice(bookId);

        return ResponseEntity.ok(finalPrice);
    }
}
