package com.poly.java5.Controller;

import com.poly.java5.Entity.User;
import com.poly.java5.Service.UserService;
import com.poly.java5.dto.ApiResponse;
import com.poly.java5.dto.LoginDTO;
import com.poly.java5.dto.RegisterDTO;
import com.poly.java5.dto.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // =============================================
    // 📝 ĐĂNG KÝ - Sử dụng RegisterDTO + Validation
    // =============================================
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterDTO dto) {
        try {
            // Chuyển DTO → Entity
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setPhone(dto.getPhone());
            user.setPassword(dto.getPassword());           // Service sẽ hash sau
            user.setFullName(dto.getFullName() != null ? dto.getFullName() : dto.getUsername());

            Map<String, String> errors = userService.register(user);

            if (errors.isEmpty()) {
                UserDTO userDTO = convertToDTO(user);
                return ResponseEntity.ok(new ApiResponse<>(true, "Đăng ký thành công", userDTO, null));
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Đăng ký thất bại", null, errors));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Lỗi server: " + e.getMessage(), null, null));
        }
    }

    // =============================================
    // 🔐 ĐĂNG NHẬP - Sử dụng LoginDTO
    // =============================================
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginDTO dto) {
        try {
            User user = userService.login(dto.getUsernameOrEmail(), dto.getPassword());

            if (user != null) {
                UserDTO userDTO = convertToDTO(user);
                return ResponseEntity.ok(new ApiResponse<>(true, "Đăng nhập thành công", userDTO, null));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(false, "Sai tài khoản hoặc mật khẩu", null, null));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Lỗi server: " + e.getMessage(), null, null));
        }
    }

    // =============================================
    // Helper: Chuyển Entity User → UserDTO (không lộ password)
    // =============================================
    private UserDTO convertToDTO(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        // dto.setAvatar(user.getAvatar());      // bỏ comment nếu Entity có method này
        // dto.setActive(user.getActive());
        return dto;
    }
}