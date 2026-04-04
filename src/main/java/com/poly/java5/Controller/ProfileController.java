package com.poly.java5.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.Config.JwtUtil;
import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
	  @Autowired
	    private UserService userService;

	    // ===== GET PROFILE =====
	    @GetMapping("")
	    public ResponseEntity<?> profile(
	            @RequestHeader("Authorization") String header) {

	        try {
	            String token = header.substring(7); // bỏ "Bearer "
	            var claims = JwtUtil.parseToken(token);

	            Integer userId = Integer.parseInt(claims.getSubject());

	            User user = userService.findById(userId);

	            return ResponseEntity.ok(user);

	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }

	    // ===== UPDATE PROFILE =====
	    @PostMapping("/update")
	    public ResponseEntity<?> updateProfile(
	            @RequestHeader("Authorization") String header,
	            @RequestBody User formUser) {

	        try {
	            String token = header.substring(7);
	            var claims = JwtUtil.parseToken(token);

	            Integer userId = Integer.parseInt(claims.getSubject());

	            User user = userService.findById(userId);

	            user.setName(formUser.getName());
	            user.setEmail(formUser.getEmail());
	            user.setPhone(formUser.getPhone());

	            userService.save(user);

	            return ResponseEntity.ok("Cập nhật thành công");

	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }
}
