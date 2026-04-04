package com.poly.java5.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ProfileController {
	@Autowired
	private UserService userService;

	// Hiển thị profile

	@GetMapping("/profile")
	public String profile(Model model, HttpServletRequest request) {

		Integer userId = Utils.getUserIdFromSession(request);

		if (userId == null) {
			return "redirect:/login";
		}

		User user = userService.findById(userId);

		model.addAttribute("user", user);

		// chọn layout theo role
		if (user.getRole() == UserRole.ADMIN) {
			model.addAttribute("layout", "admin/layout");
		} else {
			model.addAttribute("layout", "menu");
		}

		return "profile";
	}

	// Cập nhật thông tin
	@PostMapping("/profile/update")
	public String updateProfile(@ModelAttribute User formUser, HttpServletRequest request) {

		Integer userId = Utils.getUserIdFromSession(request);

		if (userId == null) {
			return "redirect:/login";
		}

		User user = userService.findById(userId);

		user.setFullName(formUser.getFullName());
		user.setEmail(formUser.getEmail());
		user.setPhone(formUser.getPhone());

		userService.updateUser(user);

		return "redirect:/profile?success";
	}
}
