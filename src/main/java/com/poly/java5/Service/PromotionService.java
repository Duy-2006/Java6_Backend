package com.poly.java5.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;
import com.poly.java5.Entity.Promotion;
import com.poly.java5.Entity.PromotionDetail;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Repository.PromotionDetailRepository;
import com.poly.java5.Repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionService {

	private final PromotionRepository promotionRepository;
    private final PromotionDetailRepository promotionDetailRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    private final LocalDate today = LocalDate.now(); // Có thể inject nếu cần timezone khác

    /**
     * Tính giá cuối cùng sau khi áp dụng khuyến mãi mạnh nhất (giá thấp nhất)
     */
    @Transactional(readOnly = true)
    public BigDecimal getFinalPrice(Integer bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal originalPrice = book.getPrice();
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return originalPrice;
        }

        // Lấy tất cả khuyến mãi đang active áp dụng cho sách này
        List<Promotion> activePromotions = getActivePromotionsForBook(book);

        if (activePromotions.isEmpty()) {
            return originalPrice;
        }

        BigDecimal finalPrice = originalPrice;
        for (Promotion promo : activePromotions) {
            BigDecimal discounted = calculateDiscountedPrice(originalPrice, promo);
            if (discounted.compareTo(finalPrice) < 0) {
                finalPrice = discounted;
            }
        }

        return finalPrice;
    }

    /**
     * Tính % giảm giá cao nhất đang áp dụng cho sách (dùng để hiển thị badge % trên card)
     * Trả về null nếu không có khuyến mãi nào
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxDiscountPercentage(Integer bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return null;
        }

        List<Promotion> activePromos = getActivePromotionsForBook(book);
        if (activePromos.isEmpty()) {
            return null;
        }

        return activePromos.stream()
                .map(Promotion::getDiscountValue)        // Lấy thẳng discountValue (đã là %)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private List<Promotion> getActivePromotionsForBook(Book book) {
        // 1. Khuyến mãi áp dụng trực tiếp cho sách (qua PromotionDetail hoặc ManyToMany)
        List<Promotion> bookPromos = promotionRepository.findActiveByBookId(book.getId());

        // 2. Khuyến mãi áp dụng cho category của sách
        Integer categoryId = (book.getCategory() != null) ? book.getCategory().getId() : null;
        List<Promotion> categoryPromos = (categoryId != null)
                ? promotionRepository.findActiveByCategoryId(categoryId)
                : List.of();

        // 3. Khuyến mãi áp dụng cho ALL
        List<Promotion> allPromos = promotionRepository.findActiveAllPromotions();

        // Gộp và loại trùng (dùng Set nếu cần, nhưng List + distinct cũng ok)
        return Stream.of(bookPromos, categoryPromos, allPromos)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal price, Promotion promo) {
        if (promo == null || promo.getDiscountValue() == null) return price;

        BigDecimal discount = price.multiply(promo.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        return price.subtract(discount);
    }

  

    @Transactional(readOnly = true)
    public List<Promotion> getAll() {
        return promotionRepository.findAll();
    }

    @Transactional
    public Promotion createPromotion(Promotion promotion, List<Integer> bookIds, List<Integer> categoryIds) {
        promotion.setStatus(true);
        Promotion saved = promotionRepository.save(promotion);
        savePromotionRelations(saved, bookIds, categoryIds);
        return saved;
    }

    @Transactional
    public Promotion updatePromotion(Promotion promotion,
                                      List<Integer> bookIds,
                                      List<Integer> categoryIds) {

        Promotion existing = promotionRepository.findById(promotion.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy promotion id = " + promotion.getId()));

        // Update thông tin cơ bản
        existing.setName(promotion.getName());
        existing.setDiscountValue(promotion.getDiscountValue());
        existing.setStartDate(promotion.getStartDate());
        existing.setEndDate(promotion.getEndDate());
        existing.setStatus(promotion.getStatus());
        existing.setApplyType(promotion.getApplyType());

        // XÓA toàn bộ detail cũ (orphanRemoval=true sẽ tự xóa DB)
        existing.getDetails().clear();

        // Thêm detail mới theo bookIds
        if (bookIds != null) {
            for (Integer id : bookIds) {
                bookRepository.findById(id).ifPresent(book -> {
                    PromotionDetail detail = new PromotionDetail();
                    detail.setPromotion(existing);
                    detail.setBook(book);
                    existing.getDetails().add(detail);
                });
            }
        }

        // Thêm detail mới theo categoryIds
        if (categoryIds != null) {
            for (Integer id : categoryIds) {
                categoryRepository.findById(id).ifPresent(category -> {
                    PromotionDetail detail = new PromotionDetail();
                    detail.setPromotion(existing);
                    detail.setCategory(category);
                    existing.getDetails().add(detail);
                });
            }
        }

        return promotionRepository.save(existing);
    }

    @Transactional
    public void deletePromotion(Integer id) {
        promotionRepository.deleteById(id);
    }

    private void savePromotionRelations(Promotion promotion, List<Integer> bookIds, List<Integer> categoryIds) {
        List<PromotionDetail> details = new ArrayList<>();

        Optional.ofNullable(bookIds).orElse(List.of()).forEach(id -> {
            bookRepository.findById(id).ifPresent(book -> {
                PromotionDetail detail = new PromotionDetail();
                detail.setPromotion(promotion);
                detail.setBook(book);
                details.add(detail);
            });
        });

        Optional.ofNullable(categoryIds).orElse(List.of()).forEach(id -> {
            categoryRepository.findById(id).ifPresent(cat -> {
                PromotionDetail detail = new PromotionDetail();
                detail.setPromotion(promotion);
                detail.setCategory(cat);
                details.add(detail);
            });
        });

        if (!details.isEmpty()) {
            promotionDetailRepository.saveAll(details);
        }
    }

    public Optional<Promotion> findById(Integer id) {
        return promotionRepository.findById(id);
    }
}