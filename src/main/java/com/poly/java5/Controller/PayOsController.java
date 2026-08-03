package com.poly.java5.Controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import com.poly.java5.DTO.CreatePaymentRequestDTO;
import com.poly.java5.DTO.PaymentResponseDTO;
import com.poly.java5.Entity.PaymentOrder;
import com.poly.java5.Repository.PaymentOrderRepository;
import com.poly.java5.Service.PayOSService;
import com.poly.java5.Utils.SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/pay-os")
@RequiredArgsConstructor
@CrossOrigin(origins = "${frontend.url}")
public class PayOsController {

    private final PayOSService payOSService;
    private final SignatureUtil signatureUtil;
    private final PaymentOrderRepository paymentOrderRepository;
    private final com.poly.java5.Service.CheckoutService checkoutService;

    // SỬA DÒNG NÀY: thêm :none
    @Value("${payos.checksum-key:none}") 
    private String checksumKey;

    // SỬA DÒNG NÀY: thêm :http://localhost:3000 (hoặc giá trị mặc định bạn muốn)
    @Value("${frontend.url:http://localhost:3000}") 
    private String frontendUrl;

    // ① Tạo link thanh toán PayOS
    @PostMapping("/create")   // → Endpoint mới: POST /api/pay-os/create
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> orderData) {
        try {
            // Lấy orderId thực từ Frontend để đồng bộ
            long orderCode = orderData.containsKey("orderId") 
                ? Long.parseLong(orderData.get("orderId").toString()) 
                : System.currentTimeMillis();
                
            int amount = Integer.parseInt(orderData.get("amount").toString());
            String description = (String) orderData.get("description");
            // Determine base URL of this backend server
            String baseUrl = "http://localhost:8080";
            try {
                baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            } catch (Exception ex) {}

            String returnUrl = orderData.containsKey("returnUrl") 
                ? (String) orderData.get("returnUrl") 
                : (baseUrl + "/api/pay-os/return");
            String cancelUrl = orderData.containsKey("cancelUrl") 
                ? (String) orderData.get("cancelUrl") 
                : (baseUrl + "/api/pay-os/return");

            CreatePaymentRequestDTO req = CreatePaymentRequestDTO.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

            PaymentResponseDTO result = payOSService.createPaymentLink(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    // ② Webhook nhận kết quả từ PayOS
    @PostMapping("/webhook")   // → Endpoint mới: POST /api/pay-os/webhook
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String receivedSig = (String) payload.get("signature");

            String expectedSig = signatureUtil.computeSignature(data, checksumKey);
            if (!expectedSig.equals(receivedSig)) {
                return ResponseEntity.status(400).body(Map.of("error", "Invalid signature"));
            }

            if ("00".equals(data.get("code"))) {
                Long orderCode = Long.parseLong(data.get("orderCode").toString());
                paymentOrderRepository.updateStatusByOrderCode(
                    orderCode,
                    PaymentOrder.PaymentStatus.PAID
                );
                
                // Cập nhật trạng thái đơn hàng thực tế
                try {
                    checkoutService.updatePaymentStatus(orderCode.intValue(), "PAID", "PAYOS-" + orderCode);
                } catch (Exception ex) {
                    System.err.println("Không thể cập nhật Order ID " + orderCode + ": " + ex.getMessage());
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ③ Xử lý khi user quay lại từ trang PayOS
    @GetMapping("/return")
    public void payosReturn(@RequestParam Map<String, String> params, HttpServletResponse response) throws java.io.IOException {
        String code = params.get("code");
        String orderCodeStr = params.get("orderCode");
        String status = params.get("status");

        if (orderCodeStr == null) {
            response.sendRedirect(frontendUrl + "/user/checkout");
            return;
        }

        Long orderCode = Long.parseLong(orderCodeStr);
        String redirectUrl = frontendUrl + "/user/checkout";

        if ("00".equals(code) || "PAID".equals(status)) {
            paymentOrderRepository.updateStatusByOrderCode(orderCode, PaymentOrder.PaymentStatus.PAID);
            
            try {
                checkoutService.updatePaymentStatus(orderCode.intValue(), "PAID", "PAYOS-" + orderCode);
                
                // Get the order to determine where to redirect
                com.poly.java5.Entity.Order order = checkoutService.getOrderById(orderCode.intValue());
                if (order != null && "audio".equalsIgnoreCase(order.getOrderType())) {
                    if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                        Integer bookId = order.getOrderDetails().iterator().next().getBook().getId();
                        redirectUrl = String.format("%s/user/books/%s/audiobook?payment=success", frontendUrl, bookId);
                    } else {
                        redirectUrl = String.format("%s/user/orders/%s/success", frontendUrl, orderCode);
                    }
                } else {
                    redirectUrl = String.format("%s/user/orders/%s/success", frontendUrl, orderCode);
                }
            } catch (Exception ex) {
                System.err.println("Không thể cập nhật Order ID " + orderCode + ": " + ex.getMessage());
                redirectUrl = String.format("%s/user/orders/%s/success", frontendUrl, orderCode);
            }
        } else {
            // failed or cancelled
            try {
                checkoutService.updatePaymentStatus(orderCode.intValue(), "FAILED", "PAYOS-" + orderCode);
            } catch (Exception ex) {
            }
            redirectUrl = frontendUrl + "/user/checkout";
        }
        
        response.sendRedirect(redirectUrl);
    }
}