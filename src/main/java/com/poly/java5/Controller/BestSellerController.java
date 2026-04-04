package com.poly.java5.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Entity.FeaturedProduct;
import com.poly.java5.Repository.OrderDetailRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/featured")
public class BestSellerController{
// doanh thu 
	 @Autowired
	    private OrderDetailRepository orderDetailRepo;

	    @GetMapping("/bestseller")
	    public String bestSeller(Model model) {

	        List<Object[]> results = orderDetailRepo.findBestSellerBooks();

	        model.addAttribute("bestSellers", results);
	        return "admin/listbook";
	    }
}
