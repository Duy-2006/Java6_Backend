package com.poly.java5.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookIndexDto {
    private Integer bookId;
    private String title;
    private String description;
    private String authorNames;
    private String categoryNames;
    private String publisherName;
    private Double price;
    private Boolean active;
    private String formats;
}
