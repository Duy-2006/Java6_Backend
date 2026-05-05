package com.poly.java5.DTO;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AuthorDTO {
	private Long id;
    @NotBlank(message = "Tên tác giả không được để trống")
    private String name;
    private String email;
    private Long bookCount;   // số lượng sách của tác giả
    
}
