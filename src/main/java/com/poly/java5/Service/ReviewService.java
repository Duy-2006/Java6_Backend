package com.poly.java5.Service;
import com.poly.java5.DTO.ReviewRequestDTO;
import com.poly.java5.DTO.ReviewResponseDTO;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Review;
import com.poly.java5.Entity.User;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.ReviewRepository;
import com.poly.java5.Repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
	private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public List<ReviewResponseDTO> getReviewsByBookId(Integer bookId) {
        return reviewRepository.findByBookIdOrderByReviewDateDesc(bookId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponseDTO addReview(Integer bookId, ReviewRequestDTO request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        Review review = Review.builder()
                .book(book)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    private ReviewResponseDTO mapToResponse(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId()).rating(review.getRating())
                .comment(review.getComment())
                .userName(review.getUser().getName()) // full name từ User entity
                .reviewDate(review.getReviewDate())
                .build();
    }
}
