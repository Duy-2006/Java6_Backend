package com.poly.java5.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 📝 ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> errors = userService.register(user);

            if (errors.isEmpty()) {
                response.put("success", true);
                response.put("message", "Đăng ký thành công");
            } else {
                response.put("success", false);
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 🔐 ĐĂNG NHẬP
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String usernameOrEmail,
            @RequestParam String password) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.login(usernameOrEmail, password);

            if (user != null) {
                response.put("success", true);
                response.put("data", user);
                response.put("message", "Đăng nhập thành công");
            } else {
                response.put("success", false);
                response.put("message", "Sai tài khoản hoặc mật khẩu");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}