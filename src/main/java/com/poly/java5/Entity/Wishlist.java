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
@Table(name = "Wishlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "id")
	    private Integer id;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "user_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private User user;
	    
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "book_id", nullable = false)
	    @ToString.Exclude
	    @EqualsAndHashCode.Exclude
	    private Book book;
	    
	    @Column(name = "added_date")
	    private LocalDateTime addedDate;
	    
	    @PrePersist
	    protected void onCreate() {
	        if (addedDate == null) {
	            addedDate = LocalDateTime.now();
	        }
	    }
}
