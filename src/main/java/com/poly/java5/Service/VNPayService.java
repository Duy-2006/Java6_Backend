package com.poly.java5.Service;

import com.poly.java5.Config.VNPayConfig;
import com.poly.java5.DTO.VNPayPaymentRequestDTO;
import com.poly.java5.Utils.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

	private final VNPayConfig vnPayConfig;

//Tạo request + ký gửi VNPay
	public String createPaymentUrl(VNPayPaymentRequestDTO request, HttpServletRequest httpRequest) throws Exception {

		// ======================
		// 1. TXN REF (ORDER ID)
		// ======================
		String vnp_TxnRef = (request.getOrderId() != null && !request.getOrderId().isEmpty()) ? request.getOrderId()
				: String.valueOf(System.currentTimeMillis());

		// ======================
		// 2. AMOUNT (x100)
		// ======================
		String vnp_Amount = String.valueOf(request.getAmount() * 100);

		// ======================
		// 3. IP ADDRESS
		// ======================
		String vnp_IpAddr = VNPayUtil.getIpAddress(httpRequest);
		if ("0:0:0:0:0:0:0:1".equals(vnp_IpAddr)) {
			vnp_IpAddr = "127.0.0.1";
		}

		// ======================
		// 4. DATE FORMAT
		// ======================
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

		String vnp_CreateDate = sdf.format(new Date());
		String vnp_ExpireDate = sdf.format(new Date(System.currentTimeMillis() + 15 * 60 * 1000));

		// ======================
		// 5. PARAMS MAP
		// ======================
		Map<String, String> params = new HashMap<>();

		params.put("vnp_Version", vnPayConfig.getVersion());
		params.put("vnp_Command", vnPayConfig.getCommand());
		params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
		params.put("vnp_Amount", vnp_Amount);
		params.put("vnp_CurrCode", vnPayConfig.getCurrency());
		params.put("vnp_TxnRef", vnp_TxnRef);

		params.put("vnp_OrderInfo", (request.getOrderInfo() != null && !request.getOrderInfo().isEmpty()) ? request.getOrderInfo() : "Thanh toan don hang " + vnp_TxnRef);
		params.put("vnp_OrderType", "other");
		params.put("vnp_Locale", vnPayConfig.getLocale());
		params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
		params.put("vnp_IpAddr", vnp_IpAddr);
		params.put("vnp_CreateDate", vnp_CreateDate);
		params.put("vnp_ExpireDate", vnp_ExpireDate);

		if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
			// params.put("vnp_BankCode", request.getBankCode());
			params.put("vnp_BankCode", "VNBANK");
		}

		// ======================
		// 6. SORT PARAMS
		// ======================
		Map<String, String> sorted = new TreeMap<>(params);

		// ======================
		// 7. BUILD RAW HASH STRING (IMPORTANT)
		// ======================
		// Sau khi đã có sorted map (TreeMap)
		StringBuilder hashData = new StringBuilder();
		for (Map.Entry<String, String> entry : sorted.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (value != null && !value.isEmpty()) {
				hashData.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()))
						.append("&");
			}
		}
		if (hashData.length() > 0) {
			hashData.setLength(hashData.length() - 1);
		}

		// ======================
		// 8. GENERATE SIGNATURE
		// ======================
		String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

		// ======================
		// 9. BUILD QUERY STRING (ENCODED)
		// ======================
		StringBuilder query = new StringBuilder();

		for (Map.Entry<String, String> entry : sorted.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			if (value != null && !value.isEmpty()) {
				query.append(URLEncoder.encode(key, StandardCharsets.US_ASCII)).append("=")
						.append(URLEncoder.encode(value, StandardCharsets.US_ASCII)).append("&");
			}
		}

		query.setLength(query.length() - 1);

		// append hash
		query.append("&vnp_SecureHash=").append(secureHash);

		// ======================
		// 10. LOG DEBUG
		// ======================
		log.info("=== VNPay CREATE PAYMENT ===");
		log.info("RAW HASH STRING: {}", hashData);
		log.info("SECURE HASH: {}", secureHash);
		log.info("TXN REF: {}", vnp_TxnRef);

		String paymentUrl = vnPayConfig.getUrl() + "?" + query;

		log.info("PAYMENT URL: {}", paymentUrl);

		return paymentUrl;
	}

	// lấy chứ ký vnpay trả về
	public boolean verifySignature(Map<String, String> params) {
		String receivedHash = params.get("vnp_SecureHash");
		log.info("Received hash: {}", receivedHash);

		Map<String, String> filtered = new TreeMap<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (value == null || value.isEmpty())
				continue;
			if (!key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
				filtered.put(key, value);
			}
		}

		log.info("Filtered params (sorted by key):");
		filtered.forEach((k, v) -> log.info("  {} = {}", k, v));

		// Quan trọng: Phải encode value theo US-ASCII giống khi tạo payment
		StringBuilder hashData = new StringBuilder();
		for (Map.Entry<String, String> entry : filtered.entrySet()) {
			if (hashData.length() > 0) {
				hashData.append("&");
			}
			hashData.append(entry.getKey()).append("=")
					.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
		}
		String hashString = hashData.toString();
		log.info("Hash string (encoded, before HMAC): {}", hashString);

		String calculated = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashString);
		log.info("Calculated hash: {}", calculated);
		log.info("Received hash : {}", receivedHash);

		boolean match = calculated.equalsIgnoreCase(receivedHash);
		log.info("Signature match: {}", match);
		return match;
	}
}