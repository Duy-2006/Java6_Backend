package com.poly.java5.DTO;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BookDTO {
	 private Integer id;

	    @NotBlank(message = "Tên sách không được để trống")
	    private String title;

	    private String isbn;

	    private String publisher;

	    @NotNull(message = "Giá không được để trống")
	    @Min(value = 0, message = "Giá phải >= 0")
	    private BigDecimal price;

	    @NotNull(message = "Số lượng không được để trống")
	    @Min(value = 0, message = "Số lượng phải >= 0")
	    private Integer quantity;

	    private BigDecimal audioPrice;

	    private Boolean active = true;

	    private String description;

	    private String imageUrl;          // để hiển thị ảnh cũ

	    private MultipartFile imageFile;  // để upload ảnh mới

	    private Long authorId;
	    private String authorName;        // chỉ để hiển thị

	    private Integer categoryId;
	    private String categoryName;      // chỉ để hiển thị   

        private Long soldCount;           // số lượng đã bán thực tế

	    private java.util.List<Long> authorIds;
	    private java.util.List<String> authorNames;
	    private java.util.List<Integer> publisherIds;
	    private java.util.List<String> publisherNames;
}
