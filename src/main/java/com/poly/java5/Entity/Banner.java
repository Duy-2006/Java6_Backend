package com.poly.java5.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Data
public class Banner {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    private String image_url; // /images/banners/banner1.jpg
	    
	    private String title; // Tên hiển thị do người dùng nhập

	    private String description; // Ghi chú do người dùng nhập

	    private String link; // /book/123 (optional)

	    private Boolean active = true;

	    private Integer position; // thứ tự hiển thị

	    private LocalDateTime start_date; // thời gian bắt đầu áp dụng

	    private LocalDateTime end_date; // thời gian kết thúc áp dụng

	    @jakarta.persistence.Column(name = "created_at", updatable = false)
	    @org.hibernate.annotations.CreationTimestamp
	    private LocalDateTime createdAt;

	    @jakarta.persistence.Column(name = "updated_at")
	    @org.hibernate.annotations.UpdateTimestamp
	    private LocalDateTime updatedAt;

}
