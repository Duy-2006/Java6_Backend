package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.FlashSaleBookDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Promotion;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")

@RequiredArgsConstructor
public class FlashSaleController {
	private final PromotionRepository promotionRepository;
	private final BookRepository bookRepository;
	private final com.poly.java5.Repository.BookFormatRepository bookFormatRepo;

	@GetMapping("/flash-sale")
	public ResponseEntity<List<FlashSaleBookDTO>> getFlashSaleBooks() {
		LocalDate today = LocalDate.now();

		List<Promotion> activePromotions = promotionRepository.findAll().stream()
				.filter(p -> p.getStatus() != null && p.getStatus())
				.filter(p -> p.getStartDate() != null && p.getEndDate() != null)
				.filter(p -> !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()))
				.collect(Collectors.toList());

		if (activePromotions.isEmpty()) {
			return ResponseEntity.ok(Collections.emptyList());
		}

		Set<Integer> discountedBookIds = new HashSet<>();
		boolean hasAllPromotion = false;

		for (Promotion promo : activePromotions) {
			// Kiểm tra nếu promotion áp dụng cho toàn sàn
			if ("ALL".equalsIgnoreCase(promo.getApplyType())) {
				hasAllPromotion = true;
				// Lấy tất cả sách (có thể lọc theo deleted = false nếu có soft delete)
				//List<Book> allBooks = bookRepository.findAll(); // hoặc findByDeletedFalse()
				// Chỉ lấy sách active = true
				List<Book> allBooks = bookRepository.findAllByActiveTrue();
				discountedBookIds.addAll(allBooks.stream().map(Book::getId).collect(Collectors.toSet()));
			} else if (promo.getDetails() != null) {
				// Xử lý các promotion áp dụng cho sách cụ thể hoặc danh mục
				promo.getDetails().stream().filter(d -> d.getBook() != null).map(d -> d.getBook().getId())
						.forEach(discountedBookIds::add);

				List<Integer> categoryIds = promo.getDetails().stream().filter(d -> d.getCategory() != null)
						.map(d -> d.getCategory().getId()).collect(Collectors.toList());

				if (!categoryIds.isEmpty()) {
					List<Book> booksInCategories = bookRepository.findByCategoryIdIn(categoryIds);
					booksInCategories.stream().map(Book::getId).forEach(discountedBookIds::add);
				}
			}
		}

		// Nếu không có sách nào thì trả về rỗng
		if (discountedBookIds.isEmpty()) {
			return ResponseEntity.ok(Collections.emptyList());
		}

		//List<Book> flashBooks = bookRepository.findAllById(discountedBookIds);
		// lấy 	theo danh sách id và active = true
		List<Book> flashBooks = bookRepository.findAllByIdInAndActiveTrue(new ArrayList<>(discountedBookIds));
		List<Integer> flashBookIds = flashBooks.stream().map(Book::getId).collect(Collectors.toList());
		
		Map<Integer, Long> soldCountMap = new HashMap<>();
		if (!flashBookIds.isEmpty()) {
			List<Object[]> soldData = bookRepository.getSoldCountByBookIds(flashBookIds);
			for (Object[] obj : soldData) {
				soldCountMap.put((Integer) obj[0], ((Number) obj[1]).longValue());
			}
		}

		Map<Integer, BigDecimal> audioPriceMap = new HashMap<>();
		if (!flashBookIds.isEmpty()) {
			List<com.poly.java5.Entity.BookFormat> formats = bookFormatRepo.findByBookIdInAndFormatType(flashBookIds, "AUDIO");
			for (com.poly.java5.Entity.BookFormat f : formats) {
				audioPriceMap.put(f.getBook().getId(), f.getPrice());
			}
		}

		List<FlashSaleBookDTO> result = new ArrayList<>();

		for (Book book : flashBooks) {
			BigDecimal bestDiscount = BigDecimal.ZERO;
			BigDecimal finalPrice = book.getPrice();
			Integer usageLimit = null;
			Integer usedCount = null;
			Integer promotionId = null;

			LocalDate promoEndDate = null;

			for (Promotion promo : activePromotions) {
				boolean applies = false;
				// Nếu promotion là ALL, tự động áp dụng cho mọi sách
				if ("ALL".equalsIgnoreCase(promo.getApplyType())) {
					applies = true;
				} else if (promo.getDetails() != null) {
					if (promo.getDetails().stream()
							.anyMatch(d -> d.getBook() != null && d.getBook().getId().equals(book.getId()))) {
						applies = true;
					} else if (promo.getDetails().stream()
							.anyMatch(d -> d.getCategory() != null && book.getCategory() != null
									&& d.getCategory().getId().equals(book.getCategory().getId()))) {
						applies = true;
					}
				}
				if (applies && promo.getDiscountValue() != null
						&& promo.getDiscountValue().compareTo(bestDiscount) > 0) {
					bestDiscount = promo.getDiscountValue();
					finalPrice = promo.applyDiscount(book.getPrice());
					usageLimit = promo.getUsageLimit();
					usedCount = promo.getUsedCount() != null ? promo.getUsedCount() : 0;
					promotionId = promo.getId();
					promoEndDate = promo.getEndDate();
				}
			}

			BigDecimal audioPrice = audioPriceMap.get(book.getId());
			Long soldCount = soldCountMap.getOrDefault(book.getId(), 0L);

			result.add(FlashSaleBookDTO.builder().id(book.getId()).title(book.getTitle()).price(book.getPrice())
					.imageUrl(book.getImageUrl()).discountValue(bestDiscount).discountPrice(finalPrice)
					.usageLimit(usageLimit)
					.usedCount(usedCount)
					.promotionId(promotionId)
					.quantity(book.getQuantity())
					.audioPrice(audioPrice)
					.soldCount(soldCount)
					.endDate(promoEndDate)
					.build());
		}

		return ResponseEntity.ok(result);
	}
}
