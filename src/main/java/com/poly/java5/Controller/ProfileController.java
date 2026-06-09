package com.poly.java5.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.DTO.UserprofileDTO;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.AuthUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
	 @Autowired private UserService userService;

	    // GET profile - trả về DTO
	    @GetMapping("")
	    public ResponseEntity<?> profile() {
	        try {
	            User user = AuthUtil.getAuthenticatedUser(userService);
	            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

	            // Map entity -> DTO
	            UserprofileDTO dto = new UserprofileDTO();
	            dto.setName(user.getName());
	            dto.setEmail(user.getEmail());
	            dto.setPhone(user.getPhone());

	            return ResponseEntity.ok(dto);
	        } catch (Exception e) {
	            return ResponseEntity.status(401).body("Unauthorized");
	        }
	    }

	    // UPDATE profile - nhận DTO, không nhận entity
	    @PostMapping("/update")
	    public ResponseEntity<?> updateProfile(@RequestBody UserprofileDTO request) {
	        try {
	            User user = AuthUtil.getAuthenticatedUser(userService);
	            if (user == null) return ResponseEntity.status(401).body("Unauthorized");

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
