package com.poly.java5.Controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalController {
	 private final UserService userService;

	    @ModelAttribute("currentUser")
	    public User addCurrentUser(HttpServletRequest request) {
	        Integer userId = Utils.getUserIdFromSession(request);

	        if (userId == null) {
	            return null;
	        }

	        return userService.findById(userId);
	    }
}
