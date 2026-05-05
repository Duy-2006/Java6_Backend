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

import com.poly.java5.DTO.UserprofileDTO;
import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.JWTService;
import com.poly.java5.Service.PromotionService;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
	 @Autowired private UserService userService;
	    @Autowired private JWTService jwtService;

	    // GET profile - trả về DTO
	    @GetMapping("")
	    public ResponseEntity<?> profile(@RequestHeader("Authorization") String header) {
	        try {
	            String token = header.substring(7);
	            if (!jwtService.validate(token))
	                return ResponseEntity.status(401).body("Invalid token");

	            Claims claims = jwtService.getBody(token);
	            Integer userId = claims.get("userId", Integer.class);
	            User user = userService.findById(userId);
	            if (user == null) return ResponseEntity.status(404).body("User not found");

	            // Map entity -> DTO
	            UserprofileDTO dto = new UserprofileDTO();
	            dto.setName(user.getName());
	            dto.setEmail(user.getEmail());
	            dto.setPhone(user.getPhone());
	            // Nếu cần id thì thêm field id vào DTO

	            return ResponseEntity.ok(dto);
	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }

	    // UPDATE profile - nhận DTO, không nhận entity
	    @PostMapping("/update")
	    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String header,
	                                           @RequestBody UserprofileDTO request) {
	        try {
	            String token = header.substring(7);
	            if (!jwtService.validate(token))
	                return ResponseEntity.status(401).body("Invalid token");

	            Claims claims = jwtService.getBody(token);
	            Integer userId = claims.get("userId", Integer.class);
	            User user = userService.findById(userId);
	            if (user == null) return ResponseEntity.status(404).body("User not found");

	            // Cập nhật từ DTO
	            user.setName(request.getName());
	            user.setEmail(request.getEmail());
	            user.setPhone(request.getPhone());
	            userService.save(user);

	            return ResponseEntity.ok("Cập nhật thành công");
	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }
}
