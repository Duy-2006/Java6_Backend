package com.poly.java5.Controller;

import java.util.List;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.BookDTO;
import com.poly.java5.Service.BookService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchApiCotroller {

	private final BookService bookService;

    @GetMapping
    public List<BookDTO> search(@RequestParam(required = false) String keyword) {

        return bookService.searchBooks(keyword).stream().map(b -> {
            BookDTO dto = new BookDTO();
            dto.setId(b.getId());
            dto.setTitle(b.getTitle());
            dto.setPrice(b.getPrice());
            dto.setImageUrl(b.getImageUrl());
            return dto;
        }).toList();
    }

}
