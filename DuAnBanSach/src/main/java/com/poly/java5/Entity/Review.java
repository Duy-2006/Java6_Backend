package com.poly.java5.Entity;
import lombok.*;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "Reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "id")
	    private Integer id;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "book_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private Book book;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "user_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private User user;
	    
	    @Column(name = "rating", nullable = false)
	    private Integer rating;  // 1-5
	    
	    @Column(name = "comment", columnDefinition = "NVARCHAR(1000)")
	    private String comment;
	    
	    @Column(name = "review_date")
	    private LocalDateTime reviewDate;
	    
	    @PrePersist
	    protected void onCreate() {
	        if (reviewDate == null) {
	            reviewDate = LocalDateTime.now();
	        }
	    }
	    
	    // Kiểm tra rating hợp lệ
	    public boolean isValidRating() {
	        return rating != null && rating >= 1 && rating <= 5;
	    }
}
