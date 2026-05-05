package com.poly.java5.Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VNPayUtil {

	// tạo chữ ký số
	public static String hmacSHA512(String key, String data) {
		try {
			Mac mac = Mac.getInstance("HmacSHA512");
			SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");

			mac.init(secretKey);
			byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();

		} catch (Exception e) {
			throw new RuntimeException("Cannot calculate HMAC-SHA512", e);
		}
	}

	// Lấy IP thật của người dùng
	public static String getIpAddress(HttpServletRequest request) {

		String ip = request.getHeader("X-FORWARDED-FOR");

		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0].trim();
		}

		return ip;
	}

	// Tạo chuỗi dữ liệu gốc (raw string) để đem đi ký.
	public static String buildRawHash(Map<String, String> params) {

		Map<String, String> sorted = new TreeMap<>(params);

		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry : sorted.entrySet()) {

			String key = entry.getKey();
			String value = entry.getValue();

			if (value != null && !value.isEmpty()) {

				sb.append(key).append("=").append(value).append("&");
			}
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	// Tạo URL query string gửi sang VNPay
	public static String buildQuery(Map<String, String> params) {

		Map<String, String> sorted = new TreeMap<>(params);

		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry : sorted.entrySet()) {

			String key = entry.getKey();
			String value = entry.getValue();

			if (value != null && !value.isEmpty()) {

				sb.append(encode(key)).append("=").append(encode(value)).append("&");
			}
		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}

		return sb.toString();
	}

	// =========================
	// URL ENCODE SAFE
	// =========================
	private static String encode(String value) {
		try {
			return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			return value;
		}
	}

	// =========================
	// DECODE RETURN PARAMS
	// =========================
	public static Map<String, String> decodeVNPayParams(Map<String, String[]> requestParams) {

		Map<String, String> decoded = new HashMap<>();

		for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {

			String key = entry.getKey();
			String[] values = entry.getValue();

			if (values != null && values.length > 0) {
				decoded.put(key, values[0]);
			}
		}

		return decoded;
	}
}