package com.poly.java5.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "banners")
@Data
public class Banner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "image_url", length = 1000, nullable = false)
    private String image_url; 

    @Column(name = "link", length = 500)
    private String link; 

    private Boolean active = true;

    private Integer position; 

    private LocalDate start_date; 

    private LocalDate end_date; 
}