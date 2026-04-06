package com.poly.java5.Controller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Service.BookService;
import com.poly.java5.Entity.Book;

@Controller
@RequiredArgsConstructor
@RequestMapping("/book")
public class ViewBookController {
	 private final BookService bookService;

	    @GetMapping("/{id}")
	    public String bookDetail(@PathVariable Integer id, Model model) {

	        try {
	            Book book = bookService.getBookById(id);
	            model.addAttribute("book", book);
	            return "book-detail"; // templates/book-detail.html

	        } catch (Exception e) {
	            return "redirect:/";
	        }
	    }
}
