package com.poly.java5.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.Bean.LoginBean;
import com.poly.java5.DTO.UserDTO;
import com.poly.java5.DTO.UserLoginDTO;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.JWTService;
import com.poly.java5.Service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth") // đổi path cho chuẩn
public class LoginController {
	@Autowired
	private UserService userService;
	
	 @Autowired
	    private JWTService jwtService;  // Inject JWTService

	//  LOGIN 
	 @PostMapping("/login")
	    public ResponseEntity<?> login(@Valid @RequestBody LoginBean loginBean,
	                                    BindingResult errors,
	                                    HttpServletResponse httpResponse) {
	        if (errors.hasErrors()) {
	            Map<String, String> errorMap = new HashMap<>();
	            errors.getFieldErrors().forEach(error ->
	                errorMap.put(error.getField(), error.getDefaultMessage())
	            );
	            return ResponseEntity.badRequest().body(Map.of("errors", errorMap));
	        }

	        User user = userService
	        		.login(loginBean.getUsernameOrEmail(), loginBean.getPassword());

	        if (user == null || !user.getActive()) {
	            return ResponseEntity.status(401)
	                    .body(Map.of("message", "Sai tài khoản hoặc mật khẩu"));
	        }
	        
	        // Dùng JWTService tạo token (24h = 86400 giây)
	        String token = jwtService.create(user, 86400);
	        System.out.println("Token created for user: " + user.getUsername());
	        
	        // ✅ Gắn JWT vào HTTP-Only Cookie (bảo mật chống XSS)
	        httpResponse.setHeader("Set-Cookie",
	            String.format("jwt=%s; Path=/; HttpOnly; Max-Age=86400; SameSite=Lax", token));
	        
	        UserDTO userRes = new UserDTO(
	        		user.getId(),
	            user.getName(),
	            user.getRole() != null ? user.getRole().name() : "USER"
	        );
	        
	        // Vẫn trả token trong body để backwards compatible với frontend hiện tại
	        UserLoginDTO response = new UserLoginDTO(token, userRes);
	        return ResponseEntity.ok(response);
	    }

	// ===== LOGOUT =====
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletResponse httpResponse) {
		// ✅ Xóa cookie bằng cách set Max-Age=0
		httpResponse.setHeader("Set-Cookie", "jwt=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax");
		return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser() {
	    var auth = SecurityContextHolder.getContext().getAuthentication();
	    
	    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
	        return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
	    }
	    
	    User user = userService.findByUsername(auth.getName());
	    
	    if (user == null) {
	        return ResponseEntity.status(404).body(Map.of("message", "User không tồn tại"));
	    }
	    
	    //  Trả về đầy đủ thông tin kèm userId
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole() != null ? user.getRole().name() : "USER");
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        response.put("avatar", user.getAvatar());

        return ResponseEntity.ok(response);
    
	}
}
