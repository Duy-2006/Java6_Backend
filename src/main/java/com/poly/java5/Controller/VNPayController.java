package com.poly.java5.Controller;

import com.poly.java5.DTO.VNPayIPNResponseDTO;
import com.poly.java5.DTO.VNPayPaymentRequestDTO;
import com.poly.java5.DTO.VNPayPaymentResponseDTO;
import com.poly.java5.Service.VNPayService;
import com.poly.java5.Service.CheckoutService;
import com.poly.java5.Utils.VNPayUtil; // ← THÊM IMPORT NÀY

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {
	private final VNPayService vnPayService;
	private final CheckoutService checkoutService;

	@org.springframework.beans.factory.annotation.Value("${frontend.url}")
	private String frontendUrl;

	/**
	 * API tạo URL thanh toán POST /api/payment/create
	 */
	@PostMapping("/create")
	public ResponseEntity<?> createPayment(@RequestBody VNPayPaymentRequestDTO request,
			HttpServletRequest httpRequest) {
		try {
			log.info("Creating payment for amount: {}", request.getAmount());

			String paymentUrl = vnPayService.createPaymentUrl(request, httpRequest);

			return ResponseEntity
					.ok(new VNPayPaymentResponseDTO(paymentUrl, request.getOrderId(), request.getAmount()));
		} catch (Exception e) {
			log.error("Error creating payment: {}", e.getMessage());
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "success", false));
		}
	}

	/**
	 * API nhận IPN (Instant Payment Notification) từ VNPay GET /api/payment/ipn
	 */
	@GetMapping("/ipn")
	public ResponseEntity<VNPayIPNResponseDTO> handleIpn(@RequestParam Map<String, String> params) {
		log.info("Received IPN with params: {}", params);

		// Xác thực chữ ký
		boolean isValid = vnPayService.verifySignature(params);

		if (!isValid) {
			log.warn("Invalid IPN signature");
			return ResponseEntity.ok(VNPayIPNResponseDTO.builder().rspCode("97").message("Invalid signature").build());
		}

		String responseCode = params.get("vnp_ResponseCode");
		String transactionNo = params.get("vnp_TransactionNo");
		String orderId = params.get("vnp_TxnRef");
		String amount = params.get("vnp_Amount");

		log.info("Transaction: orderId={}, transactionNo={}, responseCode={}", orderId, transactionNo, responseCode);

		String orderInfo = params.get("vnp_OrderInfo");
		boolean isAudiobook = (orderInfo != null && orderInfo.startsWith("audiobook_"));

		if ("00".equals(responseCode)) {
			// Cập nhật trạng thái đơn hàng trong database
			checkoutService.handleVnpayReturn(orderId, "PAID", transactionNo, isAudiobook);

			return ResponseEntity.ok(VNPayIPNResponseDTO.builder().rspCode("00").message("Success").build());
		}

		checkoutService.handleVnpayReturn(orderId, "FAILED", transactionNo, isAudiobook);
		return ResponseEntity.ok(VNPayIPNResponseDTO.builder().rspCode("01").message("Payment failed").build());
	}

	// trả về fronend 
	
	@GetMapping("/vnpay-return")
	public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// Log raw params
		request.getParameterMap().forEach((key, values) -> {
			log.info("RAW PARAM: {} = {}", key, Arrays.toString(values));
		});

		// Giải mã params
		Map<String, String> params = VNPayUtil.decodeVNPayParams(request.getParameterMap());
		log.info("VNPay return with params (decoded): {}", params);

		// Xác thực chữ ký
		boolean isValid = vnPayService.verifySignature(params);
		if (!isValid) {
			log.error("Invalid VNPay signature for order: {}", params.get("vnp_TxnRef"));
			String redirectUrl = frontendUrl + "/user/checkout";
			response.sendRedirect(redirectUrl);
			return;
		}

		String responseCode = params.get("vnp_ResponseCode");
		String transactionStatus = params.get("vnp_TransactionStatus");
		String orderId = params.get("vnp_TxnRef");
		String amount = params.get("vnp_Amount");
		long originalAmount = (amount != null && !amount.isEmpty()) ? Long.parseLong(amount) / 100 : 0;

		String orderInfo = params.get("vnp_OrderInfo"); // Example: audiobook_123_ORD...
		boolean isAudiobook = (orderInfo != null && orderInfo.startsWith("audiobook_"));

		if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
			log.info("Payment success for order: {}, amount: {}", orderId, originalAmount);
			
			// Update database status via frontend return just in case IPN is delayed
			checkoutService.handleVnpayReturn(orderId, "PAID", params.get("vnp_TransactionNo"), isAudiobook);
			
			String redirectUrl;
			if (isAudiobook) {
				// extract bookId
				String[] parts = orderInfo.split("_");
				if (parts.length >= 2) {
					String bookId = parts[1];
					redirectUrl = String.format("%s/user/books/%s/audiobook?payment=success&orderId=%s", frontendUrl, bookId, orderId);
				} else {
					redirectUrl = String.format(
							"%s/user/payment-result?status=success&orderId=%s&amount=%d&transactionNo=%s",
							frontendUrl, orderId, originalAmount, params.get("vnp_TransactionNo"));
				}
			} else {
				redirectUrl = String.format(
						"%s/user/payment-result?status=success&orderId=%s&amount=%d&transactionNo=%s",
						frontendUrl, orderId, originalAmount, params.get("vnp_TransactionNo"));
			}
			response.sendRedirect(redirectUrl);
		} else {
			log.warn("Payment failed for order: {}, ResponseCode: {}", orderId, responseCode);
			
			// Update database
			checkoutService.handleVnpayReturn(orderId, "FAILED", params.get("vnp_TransactionNo"), isAudiobook);
			
			String redirectUrl;
			if (isAudiobook) {
				String[] parts = orderInfo.split("_");
				if (parts.length >= 2) {
					String bookId = parts[1];
					redirectUrl = String.format("%s/user/books/%s/audiobook?payment=failure&orderId=%s", frontendUrl, bookId, orderId);
				} else {
					redirectUrl = frontendUrl + "/user/checkout";
				}
			} else {
				redirectUrl = frontendUrl + "/user/checkout";
			}
			response.sendRedirect(redirectUrl);
		}
	}
}