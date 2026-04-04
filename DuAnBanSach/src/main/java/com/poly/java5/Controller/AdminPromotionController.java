package com.poly.java5.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.poly.java5.Entity.Promotion;
import com.poly.java5.Repository.BookRepository;
import com.poly.java5.Repository.CategoryRepository;
import com.poly.java5.Repository.PromotionRepository;
import com.poly.java5.Service.PromotionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionRepository promotionRepository;

    // LIST
    @GetMapping
    public String list(Model model) {
        model.addAttribute("promotions", promotionService.getAll());
        model.addAttribute("today", LocalDate.now());
        return "promotion/list";
    }

    // CREATE FORM
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("promotion", new Promotion());
        model.addAttribute("books", bookRepository.findByDeletedFalse());
        model.addAttribute("categories", categoryRepository.findAll());
        return "promotion/create";
    }

    // SAVE
    @PostMapping("/save")
    public String save(@ModelAttribute Promotion promotion,
                       @RequestParam(required = false) List<Integer> bookIds,
                       @RequestParam(required = false) List<Integer> categoryIds,
                       RedirectAttributes redirectAttributes) {

        promotionService.createPromotion(promotion, bookIds, categoryIds);

        redirectAttributes.addFlashAttribute("successMessage", "Tạo khuyến mãi thành công!");
        return "redirect:/admin/promotions";
    }

//    // EDIT FORM - ĐÃ SỬA
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Promotion> optionalPromotion = promotionService.findById(id);  // ← dùng Optional

        if (optionalPromotion.isPresent()) {
            model.addAttribute("promotion", optionalPromotion.get());
            model.addAttribute("books", bookRepository.findByDeletedFalse());
            model.addAttribute("categories", categoryRepository.findAll());
            
            return "promotion/edit";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khuyến mãi với ID: " + id);
            return "redirect:/admin/promotions";
        }
    }

    // UPDATE - 
    @PostMapping("/update/{id}")
    public String updatePromotion(
            @ModelAttribute Promotion promotion,
            @RequestParam(required = false) List<Integer> bookIds,
            @RequestParam(required = false) List<Integer> categoryIds,
            RedirectAttributes redirectAttributes) {

        try {
            promotionService.updatePromotion(promotion, bookIds, categoryIds);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
    
    @GetMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("success", "Xóa khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Xóa thất bại: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}