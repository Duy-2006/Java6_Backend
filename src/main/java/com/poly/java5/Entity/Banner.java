package com.poly.java5.Entity;

import jakarta.persistence.Column;
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

    @Column(name = "image_url", length = 1000, nullable = false)
    private String image_url; 

    @Column(name = "link", length = 500)
    private String link; 

    private Boolean active = true;

    private Integer position; 

    private LocalDateTime start_date; 

    private LocalDateTime end_date; 
}