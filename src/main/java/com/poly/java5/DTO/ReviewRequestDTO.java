package com.poly.java5.DTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ReviewRequestDTO {
	@NotNull(message = "Rating không được để trống")
    @Min(1) @Max(5)
    private Integer rating;

    @NotBlank(message = "Comment không được để trống")
    private String comment;
}
