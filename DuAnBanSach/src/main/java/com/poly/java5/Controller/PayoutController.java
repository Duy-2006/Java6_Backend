package com.poly.java5.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Entity.Payout;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/payouts")
public class PayoutController {

    @GetMapping("/create")
    public String createPayoutForm(Model model) {
        model.addAttribute("payout", new Payout());
        return "payout/create";
    }

    @PostMapping("/process")
    public String processPayout(@Valid @ModelAttribute Payout payout, BindingResult result) {
        if (result.hasErrors()) {
            return "payout/create";
        }
        // Chưa logic: Xử lý chi trả
        return "redirect:/payouts/success";
    }
}
