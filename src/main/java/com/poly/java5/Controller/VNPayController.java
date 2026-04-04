package com.poly.java5.Controller;

import com.poly.java5.DTO.VNPayIPNResponseDTO;
import com.poly.java5.DTO.VNPayPaymentRequestDTO;
import com.poly.java5.DTO.VNPayPaymentResponseDTO;
import com.poly.java5.Service.VNPayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {
	 private final VNPayService vnPayService; 
	 
	 /**
	     * API tạo URL thanh toán
	     * POST /api/payment/create
	     */
	    @PostMapping("/create")
	    public ResponseEntity<?> createPayment(@RequestBody VNPayPaymentRequestDTO request, HttpServletRequest httpRequest) {
	        try {
	            log.info("Creating payment for amount: {}", request.getAmount());
	            
	            String paymentUrl = vnPayService.createPaymentUrl(request, httpRequest);
	            
	            return ResponseEntity.ok(new VNPayPaymentResponseDTO(
	                paymentUrl, 
	                request.getOrderId(), 
	                request.getAmount()
	            ));
	        } catch (Exception e) {
	            log.error("Error creating payment: {}", e.getMessage());
	            return ResponseEntity.badRequest().body(Map.of(
	                "error", e.getMessage(),
	                "success", false
	            ));
	        }
	    }

	    /**
	     * API nhận IPN (Instant Payment Notification) từ VNPay
	     * GET /api/payment/ipn
	     */
	    @GetMapping("/ipn")
	    public ResponseEntity<VNPayIPNResponseDTO> handleIpn(@RequestParam Map<String, String> params) {
	        log.info("Received IPN with params: {}", params);
	        
	        // Xác thực chữ ký
	        boolean isValid = vnPayService.verifySignature(params);
	        
	        if (!isValid) {
	            log.warn("Invalid IPN signature");
	            return ResponseEntity.ok(VNPayIPNResponseDTO.builder()
	                .rspCode("97")
	                .message("Invalid signature")
	                .build());
	        }
	        
	        String responseCode = params.get("vnp_ResponseCode");
	        String transactionNo = params.get("vnp_TransactionNo");
	        String orderId = params.get("vnp_TxnRef");
	        String amount = params.get("vnp_Amount");
	        
	        log.info("Transaction: orderId={}, transactionNo={}, responseCode={}", orderId, transactionNo, responseCode);
	        
	        if ("00".equals(responseCode)) {
	            // TODO: Cập nhật trạng thái đơn hàng trong database
	            // orderService.updatePaymentStatus(orderId, "PAID", transactionNo);
	            
	            return ResponseEntity.ok(VNPayIPNResponseDTO.builder()
	                .rspCode("00")
	                .message("Success")
	                .build());
	        }
	        
	        return ResponseEntity.ok(VNPayIPNResponseDTO.builder()
	            .rspCode("01")
	            .message("Payment failed")
	            .build());
	    }
}
