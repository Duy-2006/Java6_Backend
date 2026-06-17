package com.poly.java5.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;          // ✅ đúng, bỏ import tomcat
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.poly.java5.Config.PayOSConfig;
import com.poly.java5.DTO.CreatePaymentRequestDTO;
import com.poly.java5.DTO.PaymentResponseDTO;
import com.poly.java5.Entity.PaymentOrder;
import com.poly.java5.Repository.PaymentOrderRepository;
import com.poly.java5.Utils.SignatureUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOSConfig config;
    private final SignatureUtil signatureUtil;
    private final RestTemplate restTemplate;
    private final PaymentOrderRepository paymentOrderRepository;

    // ① Method chính — tạo link và lưu DB
    public PaymentResponseDTO createPaymentLink(CreatePaymentRequestDTO req) throws Exception {

        Map<String, Object> data = callPayOSApi(req); // ✅ đã có method bên dưới

        String checkoutUrl = (String) data.get("checkoutUrl");
        String qrCode      = (String) data.get("qrCode");

        // Lưu vào DB
        PaymentOrder order = PaymentOrder.builder()
                .orderCode(req.getOrderCode())
                .amount(req.getAmount())
                .description(req.getDescription())
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCode)
                .status(PaymentOrder.PaymentStatus.PENDING)
                .build();
        paymentOrderRepository.save(order);

        return PaymentResponseDTO.success(req.getOrderCode(), checkoutUrl, qrCode);
    }

    // ② Gọi REST API PayOS — trả về data từ response
    private Map<String, Object> callPayOSApi(CreatePaymentRequestDTO req) throws Exception {

        // Dữ liệu để tính signature (sắp xếp theo alphabet)
        Map<String, Object> signData = new LinkedHashMap<>();
        signData.put("amount",      req.getAmount());
        signData.put("cancelUrl",   req.getCancelUrl());
        signData.put("description", req.getDescription());
        signData.put("orderCode",   req.getOrderCode());
        signData.put("returnUrl",   req.getReturnUrl());

        String signature = signatureUtil.computeSignature(signData, config.getChecksumKey());

        // Request body gửi lên PayOS
        Map<String, Object> body = new HashMap<>(signData);
        body.put("items",     req.getItems());
        body.put("signature", signature);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", config.getClientId());
        headers.set("x-api-key",   config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Gọi API
        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getBaseUrl() + "/v2/payment-requests",
                entity,
                Map.class
        );

        // PayOS trả về: { code, desc, data: { checkoutUrl, qrCode, ... } }
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"00".equals(responseBody.get("code"))) {
            throw new RuntimeException("PayOS lỗi: " + 
                (responseBody != null ? responseBody.get("desc") : "Không có phản hồi"));
        }

        return (Map<String, Object>) responseBody.get("data");
    }
}