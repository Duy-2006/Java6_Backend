// SellerOrderController.java
package com.poly.java5.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/seller/order")
public class SellerOrderController {
    @GetMapping("")
    public String list(Model model) {
        model.addAttribute("activePage", "order");
        model.addAttribute("title", "Đơn hàng - BOOKSTORE");
        return "seller/order/list";
    }
}