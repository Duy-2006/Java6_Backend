// SellerProductController.java (có validation)
package com.poly.java5.Controller;

import com.poly.java5.Entity.Book; // giả sử bạn có entity Book
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller/product")
public class SellerProductController {
    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("activePage", "product");
        model.addAttribute("title", "Thêm sách - BOOKSTORE");
        model.addAttribute("book", new Book());
        return "seller/product/add";
    }

    @PostMapping("/add")
    public String save(@Valid @ModelAttribute("book") Book book, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activePage", "product");
            model.addAttribute("title", "Thêm sách - BOOKSTORE");
            return "seller/product/add";
        }
        // Lưu vào DB nếu có (tạm thời redirect)
        return "redirect:/seller/dashboard";
    }
}