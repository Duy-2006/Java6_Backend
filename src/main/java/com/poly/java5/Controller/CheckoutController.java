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
            
            return null;
        }
        
        String token = authHeader.substring(7);
        try {
            if (jwtService.validate(token)) {
                Claims claims = jwtService.getBody(token);
                String username = claims.getSubject();
                
                User user = userService.findByUsername(username);
                
                if (user != null) {
                    
                    return user.getId();
                } else {
                    
                }
            }
        } catch (Exception e) {
            
        }
        return null;
    }

    //  API XEM TRƯỚC ĐƠN HÀNG 
    @GetMapping("/preview")
    public ResponseEntity<?> previewCheckout(@RequestHeader("Authorization") String authHeader) {
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Phiên đăng nhập hết hạn hoặc không hợp lệ"));
        }

        try {
            List<Map<String, Object>> selectedItems = cartService.getSelectedCartItems(userId);
            
            if (selectedItems.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chưa có sản phẩm nào được chọn trong giỏ hàng"));
            }

            BigDecimal totalAmount = cartService.getSelectedTotalAmount(userId);
            
            // Tính phí ship: miễn phí cho đơn > 500k, ngược lại 30k
            BigDecimal shippingFee = totalAmount.compareTo(new BigDecimal("500000")) >= 0 
                ? BigDecimal.ZERO 
                : new BigDecimal("30000");
            
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
        } catch (Exception e) {
            
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    //  API TẠO ĐƠN HÀNG (CHECKOUT) 
     // Checkout - nhận thêm items từ frontend
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {  // thay vì @Valid CheckoutBean, dùng Map để nhận thêm items
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập lại"));
        }

        // Lấy dữ liệu từ payload
        String customerName = (String) payload.get("customerName");
        String customerPhone = (String) payload.get("customerPhone");
        String customerAddress = (String) payload.get("customerAddress");
        String paymentMethod = (String) payload.get("paymentMethod");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        // Validate cơ bản
        if (customerName == null || customerName.isBlank() ||
            customerPhone == null || customerPhone.isBlank() ||
            customerAddress == null || customerAddress.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng điền đầy đủ thông tin"));
        }
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không có sản phẩm nào để thanh toán"));
        }

        try {
            Order order = checkoutService.checkout(
                userId,
                customerName,
                customerPhone,
                customerAddress,
                paymentMethod,
                items   // truyền danh sách items (đã có price giảm)
            );
            return ResponseEntity.ok(Map.of(
                "success", true,
                "orderId", order.getId(),
                "orderCode", order.getOrderCode(),
                "finalAmount", order.getTotalAmount(),
                "message", "Đặt hàng thành công!"
            ));
        } catch (Exception e) {
            log.error("Checkout error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    //  API LẤY CHI TIẾT ĐƠN HÀNG 
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable Integer orderId, 
            @RequestHeader("Authorization") String authHeader) {
        
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Order order = checkoutService.getOrderById(orderId);
            
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn hàng"));
            }
            
            // Bảo mật: Kiểm tra quyền sở hữu đơn hàng
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền xem đơn hàng này"));
            }

            // Tạo DTO để trả về (tránh lộ thông tin)
            Map<String, Object> orderInfo = Map.of(
                "id", order.getId(),
                "orderCode", order.getOrderCode(),
                "totalAmount", order.getTotalAmount(),
                "status", order.getStatus(),
                "paymentStatus", order.getPaymentStatus(),
                "customerName", order.getCustomerName(),
                "customerPhone", order.getCustomerPhone(),
                "customerAddress", order.getCustomerAddress()
            );

            return ResponseEntity.ok(orderInfo);
        } catch (Exception e) {
            log.error("Lỗi lấy đơn hàng: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi: " + e.getMessage()));
        }
    }

    //  API CẬP NHẬT TRẠNG THÁI THANH TOÁN
    @PutMapping("/{orderId}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Integer orderId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body
    ) {
        Integer userId = getUserIdFromHeader(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String status = body.get("status");
        String transactionNo = body.get("transactionNo");

        try {
            // Kiểm tra quyền sở hữu đơn hàng
            Order order = checkoutService.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy đơn hàng"));
            }
            
            if (!order.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền cập nhật đơn hàng này"));
            }
            
            checkoutService.updatePaymentStatus(orderId, status, transactionNo);
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công"));
        } catch (Exception e) {
            log.error("Lỗi cập nhật thanh toán: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    //  API CẬP NHẬT TRẠNG THÁI SAU THANH TOÁN VNPAY 
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");
        String vnp_Amount = params.get("vnp_Amount");
        
        // Parse orderId từ vnp_TxnRef (vnp_TxnRef có dạng "orderId_timestamp")
        String orderIdStr = vnp_TxnRef != null ? vnp_TxnRef.split("_")[0] : null;
        
        if (orderIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transaction reference"));
        }
        
        Integer orderId = Integer.parseInt(orderIdStr);
        
        if ("00".equals(vnp_ResponseCode)) {
            // Thanh toán thành công
            checkoutService.updatePaymentStatus(orderId, "PAID", vnp_TransactionNo);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Thanh toán thành công",
                "orderId", orderId
            ));
        } else {
            // Thanh toán thất bại
            checkoutService.updatePaymentStatus(orderId, "FAILED", vnp_TransactionNo);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Thanh toán thất bại",
                "code", vnp_ResponseCode
            ));
        }
    }
}