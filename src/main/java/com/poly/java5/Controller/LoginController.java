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

@RestController
@RequestMapping("/api/auth") // đổi path cho chuẩn
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // cho Next.js gọi
public class LoginController {
	@Autowired
	private UserService userService;
	
	 @Autowired
	    private JWTService jwtService;  // Inject JWTService

	//  LOGIN 
	 @PostMapping("/login")
	    public ResponseEntity<?> login(@RequestBody LoginBean loginBean) {
	        User user = userService
	        		.login(loginBean.getUsernameOrEmail(), loginBean.getPassword());

	        if (user == null || !user.getActive()) {
	            return ResponseEntity.status(401)
	                    .body(Map.of("message", "Sai tài khoản hoặc mật khẩu"));
	        }
	        
	        // Dùng JWTService tạo token (24h = 86400 giây)
	        String token = jwtService.create(user, 86400);
	        System.out.println("Token created for user: " + user.getUsername());
	        System.out.println("Token: " + token);
	        
	        UserDTO userRes = new UserDTO(
	        		user.getId(),
	            user.getName(),
	            user.getRole().name()
	        );
	        
	        UserLoginDTO response = new UserLoginDTO(token, userRes);
	        return ResponseEntity.ok(response);
	    }

	// ===== LOGOUT =====
	@PostMapping("/logout")
	public ResponseEntity<?> logout() {
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
        Map<String, Object> response = Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "role", user.getRole().name()
        );

        return ResponseEntity.ok(response);
    
	}
//	@GetMapping("/me")
//public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
//    String authHeader = request.getHeader("Authorization");
//
//    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//        return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
//    }
//
//    String token = authHeader.substring(7);
//
//    if (JwtUtil.isExpired(token)) {
//        return ResponseEntity.status(401).body(Map.of("message", "Token expired"));
//    }
//
//    Integer userId = JwtUtil.getUserId(token);
//    User user = userService.findById(userId);
//
//    if (user == null) {
//        return ResponseEntity.status(401).body(Map.of("message", "User not found"));
//    }
//
//    UserDTO userRes = new UserDTO(
//        user.getName(),
//        user.getRole().name()
//    );
//
//    return ResponseEntity.ok(userRes);
//}
}
