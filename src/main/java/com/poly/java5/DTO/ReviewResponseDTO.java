package com.poly.java5.DTO;

import java.time.LocalDateTime;

import com.poly.java5.Entity.Review.ReviewBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ReviewResponseDTO {
	private Integer id;
    private Integer rating;
    private String comment;
    private String userName;      // Lấy từ User entity
    private LocalDateTime reviewDate;
	
	
	
}
