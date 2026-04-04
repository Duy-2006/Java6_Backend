package com.poly.java5.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Bean.RegisterBean;

@Controller
@RequestMapping("/menu")
public class MenuController {
	@GetMapping("")
	public String menu(Model model) {
		//model.addAttribute("bean",new RegisterBean());
		return "menu";
	}
}
