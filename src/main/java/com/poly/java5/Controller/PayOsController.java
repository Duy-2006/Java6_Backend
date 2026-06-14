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

    @Value("${payos.checksum-key}") 
    private String checksumKey;

    @Value("${frontend.url}") 
    private String frontendUrl;

    // ① Tạo link thanh toán PayOS
    @PostMapping("/create")   // → Endpoint mới: POST /api/pay-os/create
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> orderData) {
        try {
            long orderCode = System.currentTimeMillis();
            int amount = (int) orderData.get("amount");
            String description = (String) orderData.get("description");

            CreatePaymentRequestDTO req = CreatePaymentRequestDTO.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .returnUrl(frontendUrl + "/payment/success")
                .cancelUrl(frontendUrl + "/payment/cancel")
                .build();

            PaymentResponseDTO result = payOSService.createPaymentLink(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
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
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}