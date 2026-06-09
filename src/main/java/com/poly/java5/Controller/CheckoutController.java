package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.User;
import com.poly.java5.Service.CartService;
import com.poly.java5.Service.CheckoutService;
import com.poly.java5.Service.UserService;
import com.poly.java5.Service.VoucherService;
import com.poly.java5.Utils.AuthUtil;

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
    private final UserService userService;
    private final VoucherService voucherService;
    private final com.poly.java5.Repository.UserAddressRepository userAddressRepository;

    //  API XEM TRƯỚC ĐƠN HÀNG 
    @GetMapping("/preview")
    public ResponseEntity<?> previewCheckout() {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
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

    // API KIỂM TRA MÃ GIẢM GIÁ
    @PostMapping("/apply-voucher")
    public ResponseEntity<?> applyVoucher(
            @RequestBody Map<String, Object> payload) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập lại"));
        }

        String code = (String) payload.get("code");
        Double orderValue = 0.0;
        if (payload.get("orderValue") != null) {
            orderValue = Double.valueOf(payload.get("orderValue").toString());
        }

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mã voucher không được để trống"));
        }

        try {
            Map<String, Object> result = voucherService.applyVoucher(code, orderValue);
            // Map the result to match the frontend expectation
            return ResponseEntity.ok(Map.of(
                "success", true,
                "discountAmount", result.get("discount"),
                "finalAmount", result.get("finalAmount")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    //  API TẠO ĐƠN HÀNG (CHECKOUT) 
     // Checkout - nhận thêm items từ frontend
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody Map<String, Object> payload) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập lại"));
        }

        // Lấy dữ liệu từ payload
        String customerName = (String) payload.get("customerName");
        String customerPhone = (String) payload.get("customerPhone");
        String customerAddress = (String) payload.get("customerAddress");
        String paymentMethod = (String) payload.get("paymentMethod");
        Boolean saveAddress = payload.containsKey("saveAddress") ? (Boolean) payload.get("saveAddress") : false;
        String voucherCode = (String) payload.get("voucherCode");
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

        // Extract shipping fee
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (payload.containsKey("shippingFee") && payload.get("shippingFee") != null) {
            shippingFee = new BigDecimal(payload.get("shippingFee").toString());
        }

        try {
            // Lưu địa chỉ nếu user chọn
            if (Boolean.TRUE.equals(saveAddress)) {
                User user = userService.findById(userId);
                if (user != null) {
                    Integer provId = payload.containsKey("provinceId") && payload.get("provinceId") != null ? Integer.parseInt(payload.get("provinceId").toString()) : 0;
                    Integer distId = payload.containsKey("districtId") && payload.get("districtId") != null ? Integer.parseInt(payload.get("districtId").toString()) : 0;
                    String provName = (String) payload.get("provinceName");
                    String street = (String) payload.get("street");
                    
                    com.poly.java5.Entity.UserAddress ua = new com.poly.java5.Entity.UserAddress();
                    ua.setUser(user);
                    ua.setReceiverName(customerName);
                    ua.setReceiverPhone(customerPhone);
                    ua.setProvinceId(provId);
                    ua.setDistrictId(distId);
                    ua.setProvinceName(provName);
                    ua.setStreet(street != null ? street : customerAddress);
                    
                    List<com.poly.java5.Entity.UserAddress> existings = userAddressRepository.findByUserId(user.getId());
                    ua.setIsDefault(existings.isEmpty());
                    
                    userAddressRepository.save(ua);
                }
            }

            // Tính toán trước tổng tiền đơn hàng để apply voucher
            BigDecimal orderTotal = BigDecimal.ZERO;
            for (Map<String, Object> item : items) {
                BigDecimal price = new BigDecimal(item.get("price").toString());
                Integer qty = (Integer) item.get("quantity");
                orderTotal = orderTotal.add(price.multiply(new BigDecimal(qty)));
            }

            BigDecimal discountAmount = BigDecimal.ZERO;
            Integer appliedVoucherId = null;

            // Xử lý voucher nếu có
            if (voucherCode != null && !voucherCode.isBlank()) {
                Map<String, Object> voucherResult = voucherService.applyVoucher(voucherCode, orderTotal.doubleValue());
                discountAmount = BigDecimal.valueOf((Double) voucherResult.get("discount"));
                appliedVoucherId = (Integer) voucherResult.get("voucherId");
            }

            Order order = checkoutService.checkout(
                userId,
                customerName,
                customerPhone,
                customerAddress,
                paymentMethod,
                items,
                discountAmount,
                shippingFee
            );

            // Tăng số lượt sử dụng voucher nếu có
            if (appliedVoucherId != null) {
                voucherService.incrementUsedCount(appliedVoucherId);
            }

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

    //  API TẠO ĐƠN HÀNG TRỰC TIẾP (CHO MUA SÁCH NÓI)
    @PostMapping("/direct")
    public ResponseEntity<?> createDirectOrder(
            @RequestBody Map<String, Object> payload) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập lại"));
        }

        String customerName = (String) payload.get("customerName");
        String customerPhone = (String) payload.get("customerPhone");
        String customerAddress = (String) payload.get("customerAddress");
        String paymentMethod = (String) payload.get("paymentMethod");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không có sản phẩm nào để thanh toán"));
        }

        try {
            Order order = checkoutService.checkoutDirectly(
                userId,
                customerName != null ? customerName : "Khách Hàng Sách Nói",
                customerPhone != null ? customerPhone : "0999999999",
                customerAddress != null ? customerAddress : "Digital Delivery, VN",
                paymentMethod != null ? paymentMethod : "VNPAY",
                items,
                BigDecimal.ZERO, // No discount 
                BigDecimal.ZERO  // No shipping fee for audiobook
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "orderId", order.getId(),
                "orderCode", order.getOrderCode(),
                "finalAmount", order.getTotalAmount(),
                "message", "Đặt hàng thành công!"
            ));
        } catch (Exception e) {
            log.error("Direct checkout error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    //  API LẤY CHI TIẾT ĐƠN HÀNG 
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Integer orderId) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
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
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("id", order.getId());
            orderInfo.put("orderCode", order.getOrderCode());
            orderInfo.put("totalAmount", order.getTotalAmount());
            orderInfo.put("shippingFee", order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
            orderInfo.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
            orderInfo.put("status", order.getStatus());
            orderInfo.put("paymentStatus", order.getPaymentStatus());
            orderInfo.put("customerName", order.getCustomerName());
            orderInfo.put("customerPhone", order.getCustomerPhone());
            orderInfo.put("customerAddress", order.getCustomerAddress());

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
            @RequestBody Map<String, String> body
    ) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
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