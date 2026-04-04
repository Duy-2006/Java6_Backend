// SellerPromotionController.java
package com.poly.java5.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller/promotion")
public class SellerPromotionController {
    @GetMapping("/add")
    public String add(Model model) {
        model.addAttribute("activePage", "promotion");
        model.addAttribute("title", "Khuyến mãi - BOOKSTORE");
        return "seller/promotion/add";
    }
}