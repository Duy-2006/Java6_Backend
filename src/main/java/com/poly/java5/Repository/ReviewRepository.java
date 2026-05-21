package com.poly.java5.Repository;

import com.poly.java5.Entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
	List<Review> findByBookIdOrderByReviewDateDesc(Integer bookId);
}
