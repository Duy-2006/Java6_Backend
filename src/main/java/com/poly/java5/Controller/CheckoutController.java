package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.poly.java5.Entity.Order;
import com.poly.java5.Entity.User;
import com.poly.java5.Repository.UserAddressRepository;
import com.poly.java5.Service.CartService;
import com.poly.java5.Service.CheckoutService;
import com.poly.java5.Service.GhtkService;
import com.poly.java5.Service.UserService;
import com.poly.java5.Service.VoucherService;
import com.poly.java5.Utils.AuthUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/checkout")

@RequiredArgsConstructor
@Slf4j 
public class CheckoutController {
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final UserService userService;
    private final VoucherService voucherService;
    private final GhtkService ghtkService;
    private final UserAddressRepository userAddressRepository;

    @org.springframework.beans.factory.annotation.Value("${frontend.url}")
    private String frontendUrl;

    //  API XEM TRƯỚC ĐƠN HÀNG 
    @PostMapping("/preview")
    public ResponseEntity<?> previewCheckout(@RequestBody Map<String, Object> payload) {
        Integer userId = AuthUtil.getAuthenticatedUserId(userService);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Phiên đăng nhập hết hạn hoặc không hợp lệ"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Integer> cartDetailIds = (List<Integer>) payload.get("cartDetailIds");
            
            List<Map<String, Object>> selectedItems;
            if (cartDetailIds != null && !cartDetailIds.isEmpty()) {
                selectedItems = cartService.getCartItemsByDetails(userId, cartDetailIds);
            } else {
                selectedItems = cartService.getSelectedCartItems(userId);
            }
            
            if (selectedItems.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chưa có sản phẩm nào được chọn trong giỏ hàng"));
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Map<String, Object> item : selectedItems) {
                totalAmount = totalAmount.add((BigDecimal) item.get("itemTotal"));
            }
            
            // Tính phí ship: miễn phí cho đơn > 500k, ngược lại 30k
            BigDecimal shippingFee = totalAmount.compareTo(new BigDecimal("500000")) >= 0 
                ? BigDecimal.ZERO 
                : new BigDecimal("30000");
            
            // Tính giảm giá theo hạng thành viên
            User user = userService.findById(userId);
            int rankDiscountPercent = user != null ? user.getDiscountPercent() : 0;
            BigDecimal rankDiscount = totalAmount.multiply(BigDecimal.valueOf(rankDiscountPercent)).divide(BigDecimal.valueOf(100));

            BigDecimal discount = rankDiscount;
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
            Map<String, Object> result = voucherService.applyVoucher(code, orderValue, userId);
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

    // API TÍNH PHÍ SHIP (PREVIEW TỪ FRONTEND)
    @PostMapping("/shipping-fee")
    public ResponseEntity<?> calculateShippingFee(@RequestBody Map<String, Object> payload) {
        String provinceName = (String) payload.get("provinceName");
        String districtName = (String) payload.get("districtName");
        String wardName = (String) payload.get("wardName");
        
        
        // 🌟 CHUẨN HÓA TÊN TỈNH/THÀNH PHỐ THEO CHUẨN GHTK
        if (provinceName != null) {
            provinceName = provinceName.replace("Thành phố ", "").replace("TP. ", "").replace("Tỉnh ", "").trim();
        }
        if (districtName != null) {
            districtName = districtName.replace("Quận ", "").replace("Huyện ", "").trim();
        }
        if (wardName != null) {
            wardName = wardName.replace("Phường ", "").replace("Xã ", "").trim();
        }
        
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        if (provinceName == null || districtName == null || items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng cung cấp đầy đủ thông tin địa chỉ và giỏ hàng"));
        }

        try {
            int totalWeight = checkoutService.calculateTotalWeight(items);
            
            if (totalWeight == 0) {
                return ResponseEntity.ok(Map.of("success", true, "fee", 0));
            }

            BigDecimal orderTotal = BigDecimal.ZERO;
            for (Map<String, Object> item : items) {
                BigDecimal price = new BigDecimal(item.get("price").toString());
                Integer qty = Integer.parseInt(item.get("quantity").toString());
                orderTotal = orderTotal.add(price.multiply(new BigDecimal(qty)));
            }

            
			Double fee = ghtkService.calculateShippingFee(provinceName, districtName, wardName, totalWeight, orderTotal.doubleValue());
            return ResponseEntity.ok(Map.of("success", true, "fee", fee));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
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
        
        @SuppressWarnings("unchecked")
        List<Integer> cartDetailIds = (List<Integer>) payload.get("cartDetailIds");

        // Validate cơ bản
        if (customerName == null || customerName.isBlank() ||
            customerPhone == null || customerPhone.isBlank() ||
            customerAddress == null || customerAddress.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng điền đầy đủ thông tin"));
        }
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không có sản phẩm nào để thanh toán"));
        }

     // Extract address parameters
        String provName = (String) payload.get("provinceName");
        String distName = (String) payload.get("districtName");
        String wardName = (String) payload.get("wardName"); 
        
     // 🌟 THÊM ĐOẠN NÀY VÀO ĐỂ CHUẨN HÓA TÊN TỈNH/THÀNH PHỐ THEO CHUẨN GHTK
        if (provName != null) {
            provName = provName.replace("Thành phố ", "").replace("TP. ", "").replace("Tỉnh ", "").trim();
        }
        
        // (Tùy chọn) Chuẩn hóa cả Quận/Huyện nếu cần
        // if (districtName != null) {
        //     districtName = districtName.replace("Thành phố ", "").trim(); // Xử lý case "Thành phố Thủ Đức"
        // }

        // Xử lý Ward null (Tránh lỗi URL như vừa rồi)
        if (wardName == null || wardName.isBlank()) {
            wardName = ""; 
        }
        
        // Tính toán trước tổng tiền đơn hàng để apply voucher
        BigDecimal orderTotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal price = new BigDecimal(item.get("price").toString());
            Integer qty = Integer.parseInt(item.get("quantity").toString());
            orderTotal = orderTotal.add(price.multiply(new BigDecimal(qty)));
        }

        // BẢO MẬT: Bắt buộc Backend tự tính phí ship
        int totalWeight = checkoutService.calculateTotalWeight(items);
        BigDecimal shippingFee = BigDecimal.ZERO;
        
        if (totalWeight > 0 && provName != null && distName != null) {
            // 🌟 SỬA DÒNG NÀY: Truyền thêm wardName vào vị trí thứ 3
            Double fee = ghtkService.calculateShippingFee(provName, distName, wardName, totalWeight, orderTotal.doubleValue());
            shippingFee = BigDecimal.valueOf(fee);
        }

        try {
            // Lưu địa chỉ nếu user chọn
            if (Boolean.TRUE.equals(saveAddress)) {
                User user = userService.findById(userId);
                if (user != null) {
                    Integer provId = payload.containsKey("provinceId") && payload.get("provinceId") != null ? Integer.parseInt(payload.get("provinceId").toString()) : 0;
                    Integer distId = payload.containsKey("districtId") && payload.get("districtId") != null ? Integer.parseInt(payload.get("districtId").toString()) : 0;
                    String provname = (String) payload.get("provinceName");
                    String street = (String) payload.get("street");
                    
                    com.poly.java5.Entity.UserAddress ua = new com.poly.java5.Entity.UserAddress();
                    ua.setUser(user);
                    ua.setReceiverName(customerName);
                    ua.setReceiverPhone(customerPhone);
                    ua.setProvinceId(provId);
                    ua.setDistrictId(distId);
                    ua.setProvinceName(provName);
                    
                    // Thêm phường/xã
                    String wCode = payload.get("wardCode") != null ? payload.get("wardCode").toString() : null;
                    ua.setWardName(wardName);
                    ua.setWardCode(wCode);
                    
                    ua.setStreet(street != null ? street : customerAddress);
                    
                    List<com.poly.java5.Entity.UserAddress> existings = userAddressRepository.findByUserId(user.getId());
                    ua.setIsDefault(existings.isEmpty());
                    
                    userAddressRepository.save(ua);
                }
            }

            BigDecimal discountAmount = BigDecimal.ZERO;
            Integer appliedVoucherId = null;

            // Tính giảm giá theo hạng thành viên
            User userObj = userService.findById(userId);
            int rankDiscountPercent = userObj != null ? userObj.getDiscountPercent() : 0;
            BigDecimal rankDiscount = orderTotal.multiply(BigDecimal.valueOf(rankDiscountPercent)).divide(BigDecimal.valueOf(100));
            discountAmount = discountAmount.add(rankDiscount);

            // Xử lý voucher nếu có
            if (voucherCode != null && !voucherCode.isBlank()) {
                Map<String, Object> voucherResult = voucherService.applyVoucher(voucherCode, orderTotal.doubleValue(), userId);
                discountAmount = discountAmount.add(BigDecimal.valueOf((Double) voucherResult.get("discount")));
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
                shippingFee,
                cartDetailIds,
                rankDiscount
            );

            // Tăng số lượt sử dụng voucher nếu có
            if (appliedVoucherId != null) {
                voucherService.markVoucherAsUsedForUser(appliedVoucherId, userId);
                checkoutService.updateOrderVoucher(order.getId(), appliedVoucherId);
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
            // Tính tổng tiền sản phẩm
            BigDecimal orderTotal = BigDecimal.ZERO;
            for (Map<String, Object> item : items) {
                BigDecimal price = new BigDecimal(item.get("price").toString());
                Integer qty = Integer.parseInt(item.get("quantity").toString());
                orderTotal = orderTotal.add(price.multiply(new BigDecimal(qty)));
            }

            // Tính giảm giá theo hạng thành viên
            User userObj = userService.findById(userId);
            int rankDiscountPercent = userObj != null ? userObj.getDiscountPercent() : 0;
            BigDecimal rankDiscount = orderTotal.multiply(BigDecimal.valueOf(rankDiscountPercent)).divide(BigDecimal.valueOf(100));

            Order order = checkoutService.checkoutDirectly(
                userId,
                customerName != null ? customerName : "Khách Hàng Sách Nói",
                customerPhone != null ? customerPhone : "0999999999",
                customerAddress != null ? customerAddress : "Digital Delivery, VN",
                paymentMethod != null ? paymentMethod : "VNPAY",
                items,
                rankDiscount, // Apply customer rank discount
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
            orderInfo.put("memberDiscount", order.getMemberDiscount() != null ? order.getMemberDiscount() : BigDecimal.ZERO);
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
    public void vnpayReturn(@RequestParam Map<String, String> params, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_TransactionNo = params.get("vnp_TransactionNo");
        String vnp_Amount = params.get("vnp_Amount");
        
        // Parse orderId từ vnp_TxnRef (vnp_TxnRef có dạng "orderId_timestamp")
        String orderIdStr = vnp_TxnRef != null ? vnp_TxnRef.split("_")[0] : null;
        
        if (orderIdStr == null) {
            response.sendRedirect(frontendUrl + "/user/checkout");
            return;
        }
        
        Integer orderId = Integer.parseInt(orderIdStr);
        
        if ("00".equals(vnp_ResponseCode)) {
            // Thanh toán thành công
            checkoutService.updatePaymentStatus(orderId, "PAID", vnp_TransactionNo);
            response.sendRedirect(frontendUrl + "/user/orders/" + orderId + "/success");
        } else {
            // Thanh toán thất bại
            checkoutService.updatePaymentStatus(orderId, "FAILED", vnp_TransactionNo);
            response.sendRedirect(frontendUrl + "/user/checkout");
        }
    }
}