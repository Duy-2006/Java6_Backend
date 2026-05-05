package com.poly.java5.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poly.java5.Bean.RegisterBean;
import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.Utils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class RegisterController {
	@Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterBean bean, BindingResult errors) {

        //  Kiểm tra validation errors
        if (errors.hasErrors()) {
            Map<String, String> errorMap = new HashMap<>();
            errors.getFieldErrors().forEach(error ->
                errorMap.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(Map.of("errors", errorMap));
        }

        //  Kiểm tra confirmPassword
        if (bean.getConfirmPassword() == null || !bean.getPassword().equals(bean.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu xác nhận không khớp"));
        }

        //  Kiểm tra username tồn tại
        if (userService.findByUsername(bean.getUsername()) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username đã tồn tại"));
        }

        //  Kiểm tra email tồn tại
        if (userService.findByEmail(bean.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng"));
        }

        //  Tạo entity User từ Bean
        User user = new User();
        user.setUsername(bean.getUsername());
        user.setName(bean.getName());            
        user.setEmail(bean.getEmail());
        user.setPhone(bean.getPhone());
        
        //  KHÔNG hash ở đây - để UserService tự hash
        user.setPassword(bean.getPassword()); // Set raw password
        
        user.setActive(true);
        user.setRole(UserRole.USER);

        //  Gọi service đăng ký (service sẽ hash password)
        Map<String, String> errorsMap = userService.register(user);
        
        //  Kiểm tra lỗi từ service (nếu có)
        if (!errorsMap.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errorsMap));
        }

        //  Thành công
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đăng ký thành công! Vui lòng đăng nhập.");
        response.put("user", Map.of(
            "username", user.getUsername(),
            "name", user.getName(),
            "role", user.getRole().name()
        ));
        
        return ResponseEntity.ok(response);
    }
}
