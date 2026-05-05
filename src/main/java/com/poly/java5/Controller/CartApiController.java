package com.poly.java5.Controller;

import com.poly.java5.Entity.User;
import com.poly.java5.Service.CartService;
import com.poly.java5.Service.JWTService;
import com.poly.java5.Service.UserService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class CartApiController {
	private final CartService cartService;
	private final JWTService jwtService;
	private final UserService userService;

	// Lấy userId từ JWT token
	private Integer getUserIdFromToken(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);

			try {
				// Validate token bằng JWTService
				if (jwtService.validate(token)) {
					// Lấy claims từ token
					Claims claims = jwtService.getBody(token);

					// Lấy username từ subject
					String username = claims.getSubject();

					// Tìm user bằng username
					User user = userService.findByUsername(username);

					if (user != null) {
						Integer userId = user.getId();
						return userId;
					} else {

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
		}
		return null;
	}

	// ================= GET CART SUMMARY =================
	@GetMapping
	public ResponseEntity<?> getCart(HttpServletRequest request) {

		Integer userId = getUserIdFromToken(request);
		if (userId == null) {

			return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
		}

		try {
			Map<String, Object> cartSummary = cartService.getCartSummary(userId);

			return ResponseEntity.ok(cartSummary);
		} catch (Exception e) {

			return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
		}
	}

	// ================= ADD TO CART =================
	@PostMapping("/add")
	public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {

		try {
			Integer userId = getUserIdFromToken(request);
			if (userId == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
			}

			Integer bookId = (Integer) body.get("bookId");
			Integer quantity = body.get("quantity") != null ? (Integer) body.get("quantity") : 1;

			Map<String, Object> result = cartService.addToCart(userId, bookId, quantity);

			return ResponseEntity.ok(Map.of("success", true, "message", "Đã thêm vào giỏ hàng", "data", result));
		} catch (Exception e) {

			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ================= UPDATE CART ITEM =================
	@PostMapping("/update")
	public ResponseEntity<?> updateCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {

		try {
			Integer userId = getUserIdFromToken(request);
			if (userId == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
			}

			Integer cartDetailId = (Integer) body.get("cartDetailId");
			Integer quantity = (Integer) body.get("quantity");

			Map<String, Object> result = cartService.updateCartItem(userId, cartDetailId, quantity);

			return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thành công", "data", result));
		} catch (Exception e) {

			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ================= REMOVE FROM CART =================
	@PostMapping("/remove")
	public ResponseEntity<?> removeItem(@RequestBody Map<String, Object> body, HttpServletRequest request) {

		try {
			Integer userId = getUserIdFromToken(request);
			if (userId == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
			}

			Integer cartDetailId = (Integer) body.get("cartDetailId");

			Map<String, Object> result = cartService.removeFromCart(userId, cartDetailId);

			return ResponseEntity
					.ok(Map.of("success", true, "message", "Đã xóa sản phẩm khỏi giỏ hàng", "data", result));
		} catch (Exception e) {

			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ================= SELECT/UNSELECT ITEM =================
	@PostMapping("/select")
	public ResponseEntity<?> selectItem(@RequestBody Map<String, Object> body, HttpServletRequest request) {

		try {
			Integer userId = getUserIdFromToken(request);
			if (userId == null) {
				return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
			}

			Integer cartDetailId = (Integer) body.get("cartDetailId");
			Boolean selected = (Boolean) body.get("selected");

			cartService.updateSelected(userId, cartDetailId, selected);

			return ResponseEntity.ok(Map.of("success", true, "message", "OK"));
		} catch (Exception e) {

			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
		}
	}

	// ================= GET CART COUNT =================
	@GetMapping("/count")
	public ResponseEntity<?> count(HttpServletRequest request) {

		try {
			Integer userId = getUserIdFromToken(request);
			if (userId == null) {
				return ResponseEntity.ok(Map.of("count", 0));
			}

			int count = cartService.getCartItemCount(userId);

			return ResponseEntity.ok(Map.of("count", count));
		} catch (Exception e) {

			return ResponseEntity.ok(Map.of("count", 0));
		}
	}
}