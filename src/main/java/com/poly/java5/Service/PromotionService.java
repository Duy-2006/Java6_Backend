package com.poly.java5.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.java5.Entity.Book;

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

    // ================================================================
    // ĐỌC DỮ LIỆU
    // ================================================================

    /** Lấy toàn bộ danh sách khuyến mãi */
    @Transactional(readOnly = true)
    public List<Promotion> getAll() {
        return promotionRepository.findAll();
    }

    /** Tìm một khuyến mãi theo ID, trả về Optional để tránh NullPointerException */
    @Transactional(readOnly = true)
    public Optional<Promotion> findById(Integer id) {
        return promotionRepository.findById(id);
    }

    /**
     * Tính giá cuối cùng của sách sau khi áp dụng khuyến mãi tốt nhất.
     * Nếu có nhiều khuyến mãi → chọn cái giảm nhiều nhất (giá thấp nhất).
     * Nếu không có khuyến mãi nào → trả về giá gốc.
     */
    @Transactional(readOnly = true)
    public BigDecimal getFinalPrice(Integer bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null)
            return BigDecimal.ZERO; // sách không tồn tại

        BigDecimal originalPrice = book.getPrice();
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0)
            return originalPrice; // giá không hợp lệ → trả về nguyên

        // Lấy tất cả khuyến mãi đang active cho sách này
        List<Promotion> activePromotions = getActivePromotionsForBook(book);
        if (activePromotions.isEmpty())
            return originalPrice; // không có KM → giá gốc

        // Duyệt từng KM, giữ lại giá thấp nhất
        BigDecimal finalPrice = originalPrice;
        for (Promotion promo : activePromotions) {
            BigDecimal discounted = calculateDiscountedPrice(originalPrice, promo);
            if (discounted.compareTo(finalPrice) < 0)
                finalPrice = discounted;
        }
        return finalPrice;
    }

    /**
     * Lấy % giảm giá cao nhất đang áp dụng cho sách.
     * Dùng để hiển thị badge "-20%" trên card sách.
     * Trả về null nếu sách không có khuyến mãi nào → frontend ẩn badge.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxDiscountPercentage(Integer bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null)
            return null;

        List<Promotion> activePromos = getActivePromotionsForBook(book);
        if (activePromos.isEmpty())
            return null;

        // Lấy discountValue lớn nhất trong danh sách KM active
        return activePromos.stream()
                .map(Promotion::getDiscountValue)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Lấy danh sách sách đang được khuyến mãi (giới hạn 5 sách) để Chatbot gợi ý.
     */
    @Transactional(readOnly = true)
    public List<Book> getBooksForActivePromotions() {
        List<Promotion> activePromos = getAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()) && "ACTIVE".equals(p.getComputedStatus()))
                .toList();

        Set<Integer> bookIds = new HashSet<>();
        for (Promotion promo : activePromos) {
            if ("ALL".equalsIgnoreCase(promo.getApplyType())) {
                // Return top 5 books if it applies to all
                return bookRepository.findAll().stream().limit(5).collect(Collectors.toList());
            } else if ("BOOK".equalsIgnoreCase(promo.getApplyType())) {
                promo.getDetails().forEach(d -> {
                    if (d.getBook() != null)
                        bookIds.add(d.getBook().getId());
                });
            } else if ("CATEGORY".equalsIgnoreCase(promo.getApplyType())) {
                List<Integer> catIds = promo.getDetails().stream()
                        .filter(d -> d.getCategory() != null)
                        .map(d -> d.getCategory().getId())
                        .collect(Collectors.toList());
                if (!catIds.isEmpty()) {
                    bookRepository.findByCategoryIdIn(catIds).forEach(b -> bookIds.add(b.getId()));
                }
            }
        }

        List<Book> result = new ArrayList<>();
        for (Integer id : bookIds) {
            bookRepository.findById(id).ifPresent(result::add);
        }
        return result.stream().limit(5).collect(Collectors.toList());
    }

    // ================================================================
    // CẬP NHẬT TỒN KHO KHUYẾN MÃI VÀ KIỂM TRA
    // ================================================================

    public Promotion getBestActivePromotionForBook(Book book) {
        List<Promotion> activePromos = getActivePromotionsForBook(book);
        Promotion bestPromo = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (Promotion promo : activePromos) {
            if (promo.getDiscountValue() != null && promo.getDiscountValue().compareTo(bestDiscount) > 0) {
                bestDiscount = promo.getDiscountValue();
                bestPromo = promo;
            }
        }
        return bestPromo;
    }

    /**
     * Tăng số lượng đã dùng của khuyến mãi tốt nhất đang áp dụng cho sách.
     * Nếu vượt quá số lượng cho phép, ném Exception để chặn đặt hàng.
     */
    @Transactional
    public void incrementPromotionUsage(Integer bookId, Integer quantity) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null)
            return;

        List<Promotion> activePromos = getActivePromotionsForBook(book);
        if (activePromos.isEmpty())
            return;

        // Tìm khuyến mãi có giảm giá tốt nhất đang được áp dụng
        Promotion bestPromo = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (Promotion promo : activePromos) {
            if (promo.getDiscountValue() != null && promo.getDiscountValue().compareTo(bestDiscount) > 0) {
                bestDiscount = promo.getDiscountValue();
                bestPromo = promo;
            }
        }

        if (bestPromo != null) {
            int currentUsed = bestPromo.getUsedCount() != null ? bestPromo.getUsedCount() : 0;
            int newUsed = currentUsed + quantity;

            if (bestPromo.getUsageLimit() != null && newUsed > bestPromo.getUsageLimit()) {
                throw new RuntimeException(
                        "Chương trình khuyến mãi '" + bestPromo.getName() + "' đã hết lượt áp dụng.");
            }

            bestPromo.setUsedCount(newUsed);
            promotionRepository.save(bestPromo);
        }
    }

    // ================================================================
    // TẠO MỚI
    // ================================================================

    /**
     * Tạo mới một khuyến mãi và liên kết với sách / category.
     * Trước khi lưu sẽ kiểm tra: sách nào trong danh sách đã thuộc KM khác chưa.
     * Nếu có → ném exception, không cho tạo.
     */
    @Transactional
    public Promotion createPromotion(Promotion promotion,
            List<Integer> bookIds,
            List<Integer> categoryIds) {
        if (promotion.getDiscountValue() != null
                && promotion.getDiscountValue().compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new IllegalArgumentException("Mức giảm giá tối đa không được vượt quá 50%");
        }

        // null = đang tạo mới, không có KM nào cần bỏ qua khi kiểm tra
        validateNoOverlappingPromotions(promotion, bookIds, categoryIds, null);

        promotion.setStatus(true); // mặc định bật active khi tạo mới
        Promotion saved = promotionRepository.save(promotion);
        savePromotionRelations(saved, bookIds, categoryIds); // lưu sách & category vào detail
        return saved;
    }

    // ================================================================
    // CẬP NHẬT
    // ================================================================

    /**
     * Cập nhật thông tin khuyến mãi và danh sách sách / category áp dụng.
     * Xóa toàn bộ detail cũ → thêm lại detail mới theo danh sách truyền vào.
     * Kiểm tra trùng: bỏ qua chính KM đang sửa (sách đang thuộc KM này là hợp lệ).
     */
    @Transactional
    public Promotion updatePromotion(Promotion promotion,
            List<Integer> bookIds,
            List<Integer> categoryIds) {

        if (promotion.getDiscountValue() != null
                && promotion.getDiscountValue().compareTo(BigDecimal.valueOf(50)) > 0) {
            throw new IllegalArgumentException("Mức giảm giá tối đa không được vượt quá 50%");
        }

        // Tìm KM cần sửa, không có → báo lỗi
        Promotion existing = promotionRepository.findById(promotion.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy promotion id = " + promotion.getId()));

        // Kiểm tra sách trùng, bỏ qua chính KM này khi so sánh
        validateNoOverlappingPromotions(promotion, bookIds, categoryIds, promotion.getId());

        // Cập nhật thông tin cơ bản
        existing.setName(promotion.getName());
        existing.setDiscountValue(promotion.getDiscountValue());
        existing.setStartDate(promotion.getStartDate());
        existing.setEndDate(promotion.getEndDate());
        existing.setStatus(promotion.getStatus());
        existing.setApplyType(promotion.getApplyType());

        // Xóa toàn bộ detail cũ (orphanRemoval=true tự xóa trong DB)
        existing.getDetails().clear();

        // Thêm lại detail mới theo danh sách sách
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

        // Thêm lại detail mới theo danh sách category
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

    // ================================================================
    // XÓA
    // ================================================================

    /** Xóa khuyến mãi theo ID (các detail liên quan bị xóa theo do cascade) */
    @Transactional
    public void deletePromotion(Integer id) {
        promotionRepository.deleteById(id);
    }

    // ================================================================
    // PRIVATE HELPERS (nội bộ, không gọi từ ngoài)
    // ================================================================

    /**
     * Kiểm tra danh sách sách có cuốn nào đã thuộc khuyến mãi khác không.
     *
     * @param bookIds            danh sách ID sách cần kiểm tra
     * @param excludePromotionId ID của KM đang sửa (truyền null nếu đang tạo mới)
     *                           → bỏ qua KM này khi kiểm tra để không tự chặn chính
     *                           mình
     *
     *                           Ví dụ: KM #5 đang có sách A → khi sửa KM #5, sách A
     *                           vẫn hợp lệ
     *                           nhưng nếu sách A đang thuộc KM #3 → báo lỗi
     */
    private void validateNoOverlappingPromotions(Promotion newPromo, List<Integer> bookIds, List<Integer> categoryIds,
            Integer excludePromotionId) {
        boolean newAffectsAll = "ALL".equalsIgnoreCase(newPromo.getApplyType());
        Set<Integer> newAffectedBookIds = new HashSet<>();

        if (!newAffectsAll) {
            if ("BOOK".equalsIgnoreCase(newPromo.getApplyType()) && bookIds != null) {
                newAffectedBookIds.addAll(bookIds);
            } else if ("CATEGORY".equalsIgnoreCase(newPromo.getApplyType()) && categoryIds != null
                    && !categoryIds.isEmpty()) {
                List<Book> catBooks = bookRepository.findByCategoryIdIn(categoryIds);
                catBooks.forEach(b -> newAffectedBookIds.add(b.getId()));
            }
            if (newAffectedBookIds.isEmpty())
                return;
        }

        List<Promotion> allPromos = promotionRepository.findAll();

        for (Promotion existingPromo : allPromos) {
            if (excludePromotionId != null && existingPromo.getId().equals(excludePromotionId))
                continue;
            if (Boolean.FALSE.equals(existingPromo.getStatus()))
                continue;

            if (!isDateOverlapping(newPromo.getStartDate(), newPromo.getEndDate(), existingPromo.getStartDate(),
                    existingPromo.getEndDate())) {
                continue;
            }

            boolean existingAffectsAll = "ALL".equalsIgnoreCase(existingPromo.getApplyType());

            if (newAffectsAll || existingAffectsAll) {
                throw new IllegalStateException(
                        "Không thể áp dụng khuyến mãi vì trùng lặp thời gian với khuyến mãi Toàn Sàn: \""
                                + existingPromo.getName() + "\"");
            }

            Set<Integer> existingAffectedBookIds = new HashSet<>();
            if ("BOOK".equalsIgnoreCase(existingPromo.getApplyType())) {
                existingPromo.getDetails().forEach(d -> {
                    if (d.getBook() != null)
                        existingAffectedBookIds.add(d.getBook().getId());
                });
            } else if ("CATEGORY".equalsIgnoreCase(existingPromo.getApplyType())) {
                List<Integer> existingCatIds = existingPromo.getDetails().stream()
                        .filter(d -> d.getCategory() != null)
                        .map(d -> d.getCategory().getId())
                        .collect(Collectors.toList());
                if (!existingCatIds.isEmpty()) {
                    List<Book> catBooks = bookRepository.findByCategoryIdIn(existingCatIds);
                    catBooks.forEach(b -> existingAffectedBookIds.add(b.getId()));
                }
            }

            for (Integer bookId : newAffectedBookIds) {
                if (existingAffectedBookIds.contains(bookId)) {
                    String bookTitle = bookRepository.findById(bookId).map(Book::getTitle).orElse("ID " + bookId);
                    throw new IllegalStateException("Sách \"" + bookTitle + "\" đã thuộc chương trình khuyến mãi \""
                            + existingPromo.getName() + "\" trong cùng thời gian.");
                }
            }
        }
    }

    private boolean isDateOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null)
            return true;
        if (end1.isBefore(start2))
            return false;
        if (start1.isAfter(end2))
            return false;
        return true;
    }

    /**
     * Lấy tất cả khuyến mãi đang active áp dụng cho một cuốn sách.
     * Gồm 3 nguồn:
     * 1. KM áp dụng trực tiếp cho sách này
     * 2. KM áp dụng cho category của sách
     * 3. KM áp dụng cho tất cả sách (applyType = ALL)
     */
    private List<Promotion> getActivePromotionsForBook(Book book) {
        // Nguồn 1: KM trực tiếp cho sách
        List<Promotion> bookPromos = promotionRepository.findActiveByBookId(book.getId());

        // Nguồn 2: KM theo category (nếu sách có category)
        Integer categoryId = (book.getCategory() != null) ? book.getCategory().getId() : null;
        List<Promotion> categoryPromos = (categoryId != null)
                ? promotionRepository.findActiveByCategoryId(categoryId)
                : List.of();

        // Nguồn 3: KM áp dụng toàn bộ sách
        List<Promotion> allPromos = promotionRepository.findActiveAllPromotions();

        // Gộp 3 nguồn lại, loại bỏ trùng lặp
        return Stream.of(bookPromos, categoryPromos, allPromos)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Tính giá sách sau khi áp dụng một khuyến mãi cụ thể.
     * Công thức: giá sau = giá gốc - (giá gốc × % giảm / 100)
     * Làm tròn đến 2 chữ số thập phân.
     */
    private BigDecimal calculateDiscountedPrice(BigDecimal price, Promotion promo) {
        if (promo == null || promo.getDiscountValue() == null)
            return price;

        BigDecimal discount = price.multiply(promo.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.subtract(discount);
    }

    /**
     * Lưu danh sách sách và category vào bảng PromotionDetail.
     * Mỗi sách / category tạo thành một dòng PromotionDetail riêng.
     * Dùng khi TẠO MỚI khuyến mãi (update dùng cách khác qua
     * existing.getDetails()).
     */
    private void savePromotionRelations(Promotion promotion,
            List<Integer> bookIds,
            List<Integer> categoryIds) {
        List<PromotionDetail> details = new ArrayList<>();

        // Tạo detail cho từng sách
        Optional.ofNullable(bookIds).orElse(List.of()).forEach(id -> bookRepository.findById(id).ifPresent(book -> {
            PromotionDetail detail = new PromotionDetail();
            detail.setPromotion(promotion);
            detail.setBook(book);
            details.add(detail);
        }));

        // Tạo detail cho từng category
        Optional.ofNullable(categoryIds).orElse(List.of())
                .forEach(id -> categoryRepository.findById(id).ifPresent(cat -> {
                    PromotionDetail detail = new PromotionDetail();
                    detail.setPromotion(promotion);
                    detail.setCategory(cat);
                    details.add(detail);
                }));

        // Lưu tất cả một lần (batch insert)
        if (!details.isEmpty()) {
            promotionDetailRepository.saveAll(details);
        }
    }
}
