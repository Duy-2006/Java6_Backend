package com.poly.java5.Controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.poly.java5.DTO.CreatePaymentRequestDTO;
import com.poly.java5.DTO.PaymentResponseDTO;
import com.poly.java5.Entity.PaymentOrder;
import com.poly.java5.Repository.PaymentOrderRepository;
import com.poly.java5.Service.PayOSService;
import com.poly.java5.Utils.SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/pay-os")   // ← ĐÃ SỬA: Đổi từ /api/payment sang /api/pay-os
@RequiredArgsConstructor
@CrossOrigin(origins = "${frontend.url}")
public class PayOsController {

    private final PayOSService payOSService;
    private final SignatureUtil signatureUtil;
    private final PaymentOrderRepository paymentOrderRepository;
    private final com.poly.java5.Service.CheckoutService checkoutService;

    @Value("${payos.checksum-key}") 
    private String checksumKey;

    @Value("${frontend.url}") 
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
            String returnUrl = orderData.containsKey("returnUrl") 
                ? (String) orderData.get("returnUrl") 
                : (frontendUrl + "/user/orders/" + orderCode + "/success");
            String cancelUrl = orderData.containsKey("cancelUrl") 
                ? (String) orderData.get("cancelUrl") 
                : (frontendUrl + "/user/checkout");

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
}