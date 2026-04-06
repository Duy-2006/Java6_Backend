package com.poly.java5.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Entity.Commission;
import com.poly.java5.Entity.Revenue;
import com.poly.java5.Service.DashboardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final DashboardService dashboardService;

    @GetMapping("/list")
    public String revenuePage(Model model) {

        Map<String, Object> data = dashboardService.getDashboard();

        model.addAttribute("dashboard", data);

        return "revenue/list"; 
        // vì file nằm trong templates/revenue/list.html
    }
}
