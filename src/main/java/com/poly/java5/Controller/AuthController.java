package com.poly.java5.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Entity.User;
import com.poly.java5.Repository.UserRepository;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    UserRepository userRepo;

    @PostMapping("/dangnhap")
    public ResponseEntity<?> dangnhap(@RequestBody User request) {

        User user = userRepo.findByUsername(request.getUsername());

        if (user == null) {
            return ResponseEntity.status(401).body("Không tìm thấy user");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body("Sai mật khẩu");
        }

        if (!user.getActive()) {
            return ResponseEntity.status(403).body("Tài khoản bị khóa");
        }

        return ResponseEntity.ok(user);
    }
}