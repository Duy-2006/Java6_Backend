package com.poly.java5.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "banners")
@Data
public class Banner {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    private String image_url; // /images/banners/banner1.jpg

	    private String link; // /book/123 (optional)

	    private Boolean active = true;

	    private Integer position; // thứ tự hiển thị

}
