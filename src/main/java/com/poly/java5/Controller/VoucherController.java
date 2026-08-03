package com.poly.java5.Controller;

import com.poly.java5.DTO.*;
import com.poly.java5.Entity.User;
import com.poly.java5.Entity.Voucher;
import com.poly.java5.Service.UserService; // giả định có
import com.poly.java5.Service.UserVoucherService;
import com.poly.java5.Service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {
	private final VoucherService voucherService;
	private final UserVoucherService userVoucherService;
	private final UserService userService; // giả định có UserService
	private final com.poly.java5.Repository.OrderRepository orderRepository;

	// Helper lấy user hiện tại
	private User getCurrentUser() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userService.findByUsername(username);

		if (user == null) {
			throw new RuntimeException("User not found");
		}

		return user;
	}

	// ======================= ADMIN =======================
	@GetMapping("/admin")
	public ResponseEntity<List<VoucherResponseDTO>> getAllVouchers() {
		List<VoucherResponseDTO> responses = voucherService.findAll().stream().map(this::toResponse)
				.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/admin/{id}/orders")
	public ResponseEntity<List<Map<String, Object>>> getOrdersByVoucherId(@PathVariable Integer id) {
		List<com.poly.java5.Entity.Order> orders = orderRepository.findByVoucherIdOrderByOrderDateDesc(id);
		List<Map<String, Object>> responses = orders.stream().map(o -> {
			Map<String, Object> map = new java.util.HashMap<>();
			map.put("id", o.getId());
			map.put("orderCode", o.getOrderCode());
			map.put("customerName", o.getCustomerName());
			map.put("totalAmount", o.getTotalAmount());
			map.put("shippingFee", o.getShippingFee());
			map.put("status", o.getStatus());
			map.put("orderDate", o.getOrderDate());
			return map;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/admin/{id}")
	public ResponseEntity<VoucherResponseDTO> getVoucherById(@PathVariable Integer id) {
		Voucher v = voucherService.findById(id);
		if (v == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(toResponse(v));
	}

	@PostMapping("/admin")
	public ResponseEntity<?> createVoucher(@RequestBody VoucherRequestDTO req) {
		try {
			Map<String, Object> body = convertRequestToMap(req);
			Voucher created = voucherService.create(body);
			return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PutMapping("/admin/{id}")
	public ResponseEntity<?> updateVoucher(@PathVariable Integer id, @RequestBody VoucherRequestDTO req) {
		try {
			Map<String, Object> body = convertRequestToMap(req);
			Voucher updated = voucherService.update(id, body);
			return ResponseEntity.ok(toResponse(updated));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@DeleteMapping("/admin/{id}")
	public ResponseEntity<?> deleteVoucher(@PathVariable Integer id) {
		try {
			voucherService.delete(id);
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	// ======================= USER =======================
	@GetMapping("/active")
	public ResponseEntity<List<VoucherResponseDTO>> getActiveVouchers() {
		List<VoucherResponseDTO> responses = voucherService.findActiveVouchers().stream().map(this::toResponse)
				.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}

	@PostMapping("/apply")
	public ResponseEntity<?> applyVoucher(@RequestBody ApplyVoucherRequestDTO req) {
		try {
			Map<String, Object> result = voucherService.applyVoucher(req.getCode(), req.getOrderAmount(), null);

			ApplyVoucherResponseDTO response = ApplyVoucherResponseDTO.builder()
					.voucherId((Integer) result.get("voucherId")).code(String.valueOf(result.get("code")))
					.discountType(String.valueOf(result.get("discountType")))
					.discount(((Number) result.get("discount")).doubleValue())
					.finalAmount(((Number) result.get("finalAmount")).doubleValue()).build();

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/claim/{voucherId}")
	public ResponseEntity<?> claimVoucher(@PathVariable Integer voucherId) {
		try {
			User user = getCurrentUser();
			userVoucherService.claimVoucher(user, voucherId);
			return ResponseEntity.ok(Map.of("message", "Nhận voucher thành công"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/my-vouchers")
	public ResponseEntity<List<UserVoucherDTO>> getMyVouchers() {
		User user = getCurrentUser();
		List<UserVoucherDTO> dtos = userVoucherService
				.getAllUserVouchers(user).stream().map(uv -> UserVoucherDTO.builder().userVoucherId(uv.getId())
						.voucher(toResponse(uv.getVoucher())).isUsed(uv.getIsUsed()).usedDate(uv.getUsedDate()).build())
				.collect(Collectors.toList());
		return ResponseEntity.ok(dtos);
	}

	// Helper methods
	private VoucherResponseDTO toResponse(Voucher v) {
		return VoucherResponseDTO.builder().id(v.getId()).code(v.getCode()).discountType(v.getDiscountType())
				.discountValue(v.getDiscountValue()).minOrderValue(v.getMinOrderValue()).maxDiscount(v.getMaxDiscount())
				.usageLimit(v.getUsageLimit()).usedCount(v.getUsedCount()).startDate(v.getStartDate())
				.endDate(v.getEndDate()).active(v.isActive()).status(v.getComputedStatus()).build();
	}

	private Map<String, Object> convertRequestToMap(VoucherRequestDTO req) {
		Map<String, Object> map = new java.util.HashMap<>();
		map.put("code", req.getCode());
		map.put("discountType", req.getDiscountType());
		map.put("discountValue", req.getDiscountValue());
		map.put("minOrderValue", req.getMinOrderValue());
		map.put("maxDiscount", req.getMaxDiscount());
		map.put("usageLimit", req.getUsageLimit());
		map.put("startDate", req.getStartDate());
		map.put("endDate", req.getEndDate());
		map.put("active", req.getActive());
		return map;
	}
}
