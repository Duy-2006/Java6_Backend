// SellerFinanceController.java
package com.poly.java5.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller/finance")
public class SellerFinanceController {
    @GetMapping("")
    public String report(Model model) {
        model.addAttribute("activePage", "finance");
        model.addAttribute("title", "Tài chính - BOOKSTORE");
        return "seller/finance/report";
    }
}