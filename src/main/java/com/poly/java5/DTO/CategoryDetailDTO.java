package com.poly.java5.DTO;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CategoryDetailDTO {
	private Integer id;
    private String name;
    private List<BookDTO> books;
    private Long bookCount;  // thêm trường này
}
