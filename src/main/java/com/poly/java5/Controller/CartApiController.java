package com.poly.java5.Controller;

import com.poly.java5.Config.JwtUtil;
import com.poly.java5.Service.BookService;
import com.poly.java5.Service.CartService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class CartApiController {
	private final CartService cartService;

    // ✅ Lấy userId từ JWT token trong cookie
    private Integer getUserIdFromJWT(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("JWT".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !JwtUtil.isExpired(token)) {
                        return JwtUtil.getUserId(token);  // Trả về Integer
                    }
                }
            }
        }
        return null;
    }

    // ================= GET CART =================
    @GetMapping
    public ResponseEntity<?> getCart(HttpServletRequest request) {
        Integer userId = getUserIdFromJWT(request);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }
        return ResponseEntity.ok(cartService.getCartSummary(userId));
    }

    // ================= ADD =================
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body,
                                       HttpServletRequest request) {
        try {
            Integer userId = getUserIdFromJWT(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
            }

            Integer bookId = (Integer) body.get("bookId");
            Integer quantity = body.get("quantity") != null
                    ? (Integer) body.get("quantity") : 1;

            cartService.addToCart(userId, bookId, quantity);

            return ResponseEntity.ok(Map.of(
                "message", "Đã thêm vào giỏ",
                "cartCount", cartService.getCartItemCount(userId)
            ));
        } catch (Exception e) {
            log.error("Add cart error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ================= UPDATE (Sửa thành POST cho đồng bộ với frontend) =================
    @PostMapping("/update")
    public ResponseEntity<?> updateCart(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        try {
            Integer userId = getUserIdFromJWT(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
            }

            Integer cartDetailId = (Integer) body.get("cartDetailId");
            Integer quantity = (Integer) body.get("quantity");

            Map<String, Object> result = cartService.updateCartItem(userId, cartDetailId, quantity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Update cart error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ================= REMOVE (Sửa thành POST) =================
    @PostMapping("/remove")
    public ResponseEntity<?> removeItem(@RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        try {
            Integer userId = getUserIdFromJWT(request);
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
            }

            Integer cartDetailId = Integer.parseInt(body.get("cartDetailId"));
            cartService.removeFromCart(userId, cartDetailId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm"));
        } catch (Exception e) {
            log.error("Remove error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ================= SELECT (Sửa thành POST) =================
    @PostMapping("/select")
    public ResponseEntity<?> selectItem(@RequestBody Map<String, Object> body,
                                        HttpServletRequest request) {
        Integer userId = getUserIdFromJWT(request);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        }

        Integer cartDetailId = (Integer) body.get("cartDetailId");
        Boolean selected = (Boolean) body.get("selected");

        cartService.updateSelected(userId, cartDetailId, selected);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    // ================= COUNT =================
    @GetMapping("/count")
    public ResponseEntity<?> count(HttpServletRequest request) {
        Integer userId = getUserIdFromJWT(request);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

}
