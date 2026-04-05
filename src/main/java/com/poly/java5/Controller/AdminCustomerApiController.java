package com.poly.java5.Controller;

import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/customers")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminCustomerApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    // GET /api/admin/customers - Danh sách tất cả khách hàng
    @GetMapping
    public ResponseEntity<?> getAllCustomers() {
        List<User> users = userService.findByRole(UserRole.USER);

        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : users) {
            Double spending = orderService.sumSpendingByUsername(u.getUsername());
            if (spending == null) spending = 0.0;

            // Phân loại khách hàng
            String type;
            if (spending >= 5_000_000) {
                type = "VIP (Thân thiết)";
            } else if (spending >= 1_000_000) {
                type = "Tiềm năng";
            } else {
                type = "Khách mới";
            }

            Map<String, Object> item = new HashMap<>();
            item.put("username", u.getUsername());
            item.put("fullName", u.getName());
            item.put("email", u.getEmail());
            item.put("phone", u.getPhone());
            item.put("active", u.getActive());           // Sửa: dùng getActive() thay vì isActive()
            item.put("totalSpending", spending);
            item.put("customerType", type);
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // GET lịch sử mua hàng của một khách hàng
    @GetMapping("/history/{username}")
    public ResponseEntity<?> getHistory(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Double spending = orderService.sumSpendingByUsername(username);
        List<?> orders = orderService.findByUsername(username);

        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("fullName", user.getName());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("active", user.getActive());
        result.put("totalSpending", spending != null ? spending : 0.0);
        result.put("orders", orders);

        return ResponseEntity.ok(result);
    }

    // PUT Toggle khóa / mở khóa tài khoản
    @PutMapping("/toggle/{username}")
    public ResponseEntity<?> toggleStatus(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        boolean newStatus = !user.getActive();   // Lấy ngược trạng thái hiện tại
        user.setActive(newStatus);
        userService.save(user);

        return ResponseEntity.ok(Map.of(
            "message", newStatus ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản",
            "active", newStatus,
            "username", username
        ));
    }
}