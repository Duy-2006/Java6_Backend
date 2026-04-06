package com.poly.java5.Controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.poly.java5.Bean.RegisterBean;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/register")
public class RegisterController {
	private final UserService userService;

	public RegisterController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("")
	public String registerUI(Model model) {
		model.addAttribute("bean", new RegisterBean());
		return "register";
	}

	@PostMapping("")
	public String register(@Valid @ModelAttribute("bean") RegisterBean bean,
	                       Errors errors,
	                       Model model) {

	    if (errors.hasErrors()) {
	        return "register";
	    }

	    // Convert Bean → Entity
	    User user = new User();
	    user.setUsername(bean.getUsername());
	    user.setEmail(bean.getEmail());
	    user.setPhone(bean.getPhone());
	    user.setPassword(bean.getPassword());
	    user.setFullName(bean.getName());
	    user.setActive(true);

	    // Gọi service
	    Map<String, String> serviceErrors = userService.register(user);

	    if (!serviceErrors.isEmpty()) {
	        serviceErrors.forEach((field, message) ->
	                errors.rejectValue(field, "", message)
	        );
	        return "register";
	    }

	    return "redirect:/login";
	}

}
