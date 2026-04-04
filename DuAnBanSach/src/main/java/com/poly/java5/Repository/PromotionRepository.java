package com.poly.java5.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.poly.java5.Entity.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    
	@Query("""
		    SELECT d.promotion
		    FROM PromotionDetail d
		    WHERE d.book.id = :bookId
		    AND d.promotion.status = true
		    AND CURRENT_DATE BETWEEN d.promotion.startDate AND d.promotion.endDate
		""")
		List<Promotion> findActiveByBookId(Integer bookId);

		@Query("""
		    SELECT d.promotion
		    FROM PromotionDetail d
		    WHERE d.category.id = :categoryId
		    AND d.promotion.status = true
		    AND CURRENT_DATE BETWEEN d.promotion.startDate AND d.promotion.endDate
		""")
		List<Promotion> findActiveByCategoryId(Integer categoryId);

		@Query("""
			    SELECT p FROM Promotion p
			    WHERE p.status = true
			    AND CURRENT_DATE BETWEEN p.startDate AND p.endDate
			    AND p.applyType = 'ALL'
			""")
			List<Promotion> findActiveAllPromotions();
}

