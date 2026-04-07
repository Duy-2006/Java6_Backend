package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Bean.CheckoutBean;
import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.CartService;
import com.poly.java5.Service.CheckoutService;
import com.poly.java5.Service.JWTService;
import com.poly.java5.Service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j 
public class CheckoutController {
	private final CartService cartService;
    private final CheckoutService checkoutService;
    private final JWTService jwtService;
    private final UserService userService;

    /**
     * Hàm hỗ trợ lấy UserId từ Token trong Header
     */
    private Integer getUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Header Authorization trống hoặc sai định dạng");
            return null;
        }
        
        String token = authHeader.substring(7);
        try {
            if (jwtService.validate(token)) {
                Claims claims = jwtService.getBody(token);
                String username = claims.getSubject(); // Lấy "Duy"
                
                
				// Tìm User trong Database dựa trên username
                User user = userService.findByUsername(username);
                
                if (user != null) {
                    log.info("Xác thực thành công cho User: {} (ID: {})", username, user.getId());
                    return user.getId(); // Trả về ID kiểu Integer chuẩn
                } else {
                    log.error("Token hợp lệ nhưng không tìm thấy User '{}' trong Database", username);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý Token: {}", e.getMessage());
        }
        return null;
    }

    // ================= 1. API XEM TRƯỚC ĐƠN HÀNG =================
    @GetMapping("/preview")
    public ResponseEntity<?> previewCheckout(@RequestHeader("Authorization") String authHeader) {
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Phiên đăng nhập hết hạn hoặc không hợp lệ"));
        }

        List<Map<String, Object>> selectedItems = cartService.getSelectedCartItems(userId);
        if (selectedItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chưa có sản phẩm nào được chọn trong giỏ hàng"));
        }

        BigDecimal totalAmount = cartService.getSelectedTotalAmount(userId);
        BigDecimal shippingFee = BigDecimal.valueOf(15000);
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discount);

        Map<String, Object> response = Map.of(
            "cartDetails", selectedItems,
            "totalAmount", totalAmount,
            "shippingFee", shippingFee,
            "discount", discount,
            "finalAmount", finalAmount
        );

        return ResponseEntity.ok(response);
    }

    // ================= 2. API TẠO ĐƠN HÀNG (CHECKOUT) =================
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CheckoutBean bean,
            BindingResult result
    ) {
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập lại"));
        }

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }

        try {
            Order order = checkoutService.checkout(
                userId,
                bean.getCustomerName(),
                bean.getCustomerPhone(),
                bean.getCustomerAddress(),
                bean.getPaymentMethod()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "orderId", order.getId(),
                "orderCode", order.getOrderCode(),
                "finalAmount", order.getTotalAmount(),
                "message", "Đặt hàng thành công!"
            ));
        } catch (Exception e) {
            log.error("Lỗi khi đặt hàng: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ================= 3. API LẤY CHI TIẾT ĐƠN HÀNG =================
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable Integer orderId, 
            @RequestHeader("Authorization") String authHeader) {
        
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        try {
            Order order = checkoutService.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn hàng"));
            }
            
            // Bảo mật: Kiểm tra xem đơn hàng có thuộc về user đang đăng nhập không
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem đơn hàng này"));
            }

            return ResponseEntity.ok(order); // Hoặc map sang DTO để tránh lộ thông tin User
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    // ================= 4. API CẬP NHẬT TRẠNG THÁI THANH TOÁN =================
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Integer orderId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String status = body.get("status");
        String transactionNo = body.get("transactionNo");

        try {
            // Có thể thêm bước kiểm tra quyền sở hữu order ở đây trước khi update
            checkoutService.updatePaymentStatus(orderId, status, transactionNo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
