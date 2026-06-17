package com.poly.java5.Controller;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/profile")

public class AdminProfileApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";

    // 1. GET admin profile
    @GetMapping("")
    public ResponseEntity<?> getAdminProfile() {
        User user = AuthUtil.getAuthenticatedUser(userService);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Không có quyền truy cập"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone() != null ? user.getPhone() : "");
        data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        return ResponseEntity.ok(data);
    }

    // 2. PUT admin profile update (text fields only)
    @PutMapping(value = "/update", consumes = "application/json")
    public ResponseEntity<?> updateAdminProfile(@RequestBody Map<String, String> jsonBody) {
        User user = AuthUtil.getAuthenticatedUser(userService);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Không có quyền truy cập"));
        }

        String updatedName = jsonBody.get("name");
        String updatedEmail = jsonBody.get("email");
        String updatedPhone = jsonBody.get("phone");

        if (updatedName != null && !updatedName.trim().isEmpty()) {
            user.setName(updatedName);
        }
        if (updatedEmail != null && !updatedEmail.trim().isEmpty()) {
            user.setEmail(updatedEmail);
        }
        if (updatedPhone != null) {
            user.setPhone(updatedPhone);
        }

        userService.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone() != null ? user.getPhone() : "");
        data.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        data.put("message", "Cập nhật thông tin thành công");

        return ResponseEntity.ok(data);
    }

    // 2b. PUT admin profile avatar upload
    @PutMapping(value = "/update-avatar", consumes = "multipart/form-data")
    public ResponseEntity<?> updateAdminAvatar(@RequestParam("imageFile") MultipartFile imageFile) {
        User user = AuthUtil.getAuthenticatedUser(userService);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Không có quyền truy cập"));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String filename = saveImage(imageFile);
                user.setAvatar("/uploads/avatars/" + filename);
            } catch (IOException e) {
                return ResponseEntity.status(500).body(Map.of("message", "Lỗi lưu ảnh: " + e.getMessage()));
            }
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy file ảnh tải lên"));
        }

        userService.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("avatar", user.getAvatar());
        data.put("message", "Đã cập nhật ảnh đại diện thành công");

        return ResponseEntity.ok(data);
    }

    // 3. PUT change password
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        User user = AuthUtil.getAuthenticatedUser(userService);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("message", "Không có quyền truy cập"));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isEmpty() ||
            newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu mật khẩu hiện tại hoặc mật khẩu mới"));
        }

        // Kiểm tra mật khẩu hiện tại
        String hashedCurrent = com.poly.java5.Utils.Utils.hashPassword(currentPassword);
        if (hashedCurrent == null || !hashedCurrent.equals(user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu hiện tại không chính xác"));
        }

        // Mã hóa mật khẩu mới và lưu
        user.setPassword(com.poly.java5.Utils.Utils.hashPassword(newPassword));
        userService.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
