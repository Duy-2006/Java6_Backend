package com.poly.java5.Controller;

import com.poly.java5.Service.BookService;
import com.poly.java5.Service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
	private final CartService cartService;
	private final BookService bookService;

	private Integer getUserId(HttpSession session) {
		return (Integer) session.getAttribute("USER_ID");
	}

	// ============ VIEW PAGES ============

	// Trang chủ sản phẩm
	@GetMapping("/products")
	public String viewProducts(Model model, HttpSession session) {
		log.info("Hiển thị trang sản phẩm");

		Integer userId = getUserId(session); // khởi tạo session
		if (userId == null) {
			return "redirect:/login";
		}
		// lấy danh sách sách , lấy số lượng sản phẩm trong giỏ hàng 
		model.addAttribute("books", bookService.getAllBooks());
		int cartCount = cartService.getCartItemCount(userId);
		model.addAttribute("cartCount", cartCount);

		return "cart/products";
	}

	// Trang giỏ hàng
	@GetMapping
	public String viewCart(Model model, HttpSession session) {
		log.info("Hiển thị trang giỏ hàng");

		Integer userId = getUserId(session);
		if (userId == null) {
			return "redirect:/login";
		}

		Map<String, Object> cartSummary = cartService.getCartSummary(userId);

		model.addAttribute("cart", cartSummary.get("cart"));
		model.addAttribute("cartItems", cartSummary.get("cartItems"));
		model.addAttribute("totalItems", cartSummary.get("totalItems"));
		model.addAttribute("totalAmount", cartSummary.get("totalAmount"));
		model.addAttribute("finalAmount", cartSummary.get("finalAmount"));

		return "cart";
	}

	// ============ API ENDPOINTS ============

	// API: Thêm vào giỏ hàng
	// sản phẩm được lưu và db
	//	User click "Add to cart"
	//JavaScript gọi API /cart/api/add
	//CartController
	//CartService.addToCart()
	//Kiểm tra book
	//Lấy hoặc tạo Cart
	//Kiểm tra đã tồn tại CartDetail chưa
	//Insert hoặc Update CartDetail
	//Update thời gian Cart
	//Trả JSON về frontend
	@PostMapping("/api/add")
	@ResponseBody
	public Map<String, Object> addToCartApi(@RequestParam Integer bookId,
			@RequestParam(defaultValue = "1") Integer quantity, HttpSession session) {

		log.info("API: Thêm vào giỏ hàng - bookId: {}, quantity: {}", bookId, quantity);

		Map<String, Object> response = new HashMap<>();

		try {
			Integer userId = getUserId(session);
			if (userId == null) {
				throw new RuntimeException("Chưa đăng nhập");
			}

			// Chỉ gọi service, KHÔNG cần lấy result
			cartService.addToCart(userId, bookId, quantity);

			response.put("success", true);
			response.put("message", "Đã thêm vào giỏ hàng");
			response.put("cartCount", cartService.getCartItemCount(userId));

		} catch (Exception e) {
			log.error("Lỗi khi thêm vào giỏ hàng: ", e);
			response.put("success", false);
			response.put("message", e.getMessage());
		}

		return response;
	}

	// API: Cập nhật số lượng
	@PostMapping("/api/update")
	@ResponseBody
	public Map<String, Object> updateCartApi(@RequestParam Integer cartDetailId, @RequestParam Integer quantity,
			HttpSession session) {

		log.info("API: Cập nhật giỏ hàng - cartDetailId: {}, quantity: {}", cartDetailId, quantity);

		Map<String, Object> response = new HashMap<>();

		try {
			Integer userId = getUserId(session);
			if (userId == null) {
				throw new RuntimeException("Chưa đăng nhập");
			}
			Map<String, Object> result = cartService.updateCartItem(userId, cartDetailId, quantity);

			response.put("success", true);
			response.put("message", "Đã cập nhật giỏ hàng");
			response.put("data", result);

		} catch (Exception e) {
			log.error("Lỗi khi cập nhật giỏ hàng: ", e);
			response.put("success", false);
			response.put("message", e.getMessage());
		}

		return response;
	}

	// API: Xóa sản phẩm
	// bấm núc xóa thì gọi api contronller nhận request 
	//kiểm tra đăng hập gọi sevice sử lý nó sẻ tim sản phẩm đó nếu có thì tiếp tục (không có thì trả về cảnh báo)
	// kiểm tra userid nếu đúng thì xóa khỏi giỏ hàng 
	@PostMapping("/api/remove")
	@ResponseBody
	public Map<String, Object> removeFromCartApi(
	        @RequestParam Integer cartDetailId,
	        HttpSession session) {

	    log.info("API: Xóa khỏi giỏ hàng - cartDetailId: {}", cartDetailId);

	    Map<String, Object> response = new HashMap<>();

	    try {
	        Integer userId = getUserId(session);
	        if (userId == null) {
	            throw new RuntimeException("Chưa đăng nhập");
	        }

	        cartService.removeFromCart(userId, cartDetailId);

	        response.put("success", true);
	        response.put("message", "Đã xóa sản phẩm");

	    } catch (Exception e) {
	        log.error("Lỗi khi xóa khỏi giỏ hàng: ", e);
	        response.put("success", false);
	        response.put("message", e.getMessage());
	    }

	    return response;
	}

	// API: Xóa toàn bộ giỏ hàng
	@PostMapping("/api/clear")
	@ResponseBody
	public Map<String, Object> clearCartApi(HttpSession session) {
		log.info("API: Xóa toàn bộ giỏ hàng");

		Map<String, Object> response = new HashMap<>();

		try {
			Integer userId = getUserId(session);
			if (userId == null) {
				throw new RuntimeException("Chưa đăng nhập");
			}
			cartService.clearCart(userId);

			response.put("success", true);
			response.put("message", "Đã xóa toàn bộ giỏ hàng");

		} catch (Exception e) {
			log.error("Lỗi khi xóa giỏ hàng: ", e);
			response.put("success", false);
			response.put("message", e.getMessage());
		}

		return response;
	}

	// API: Lấy thông tin giỏ hàng (dùng khi loat lại trang bằng  AJAX)
	@GetMapping("/api/summary")
	@ResponseBody
	public Map<String, Object> getCartSummaryApi(HttpSession session) {
		log.info("API: Lấy thông tin giỏ hàng");

		Integer userId = getUserId(session);
		if (userId == null) {
			throw new RuntimeException("Chưa đăng nhập");
		}
		return cartService.getCartSummary(userId);
	}

	// API: Đếm số lượng
	@GetMapping("/api/count")
	@ResponseBody
	public Map<String, Object> getCartCountApi(HttpSession session) {
		log.info("API: Đếm số lượng sản phẩm trong giỏ");

		Integer userId = getUserId(session);
		int count = cartService.getCartItemCount(userId);

		Map<String, Object> response = new HashMap<>();
		response.put("count", count);
		response.put("userId", userId);

		return response;
	}
	// cập nhật trạng thái 
	@PostMapping("/select")
	@ResponseBody
	public void selectCartItem(@RequestBody Map<String, Object> body, HttpSession session) {
		Integer userId = getUserId(session);
		if (userId == null) {
			throw new RuntimeException("Chưa đăng nhập");
		}

		Integer cartDetailId = Integer.valueOf(body.get("cartDetailId").toString());

		Boolean selected = Boolean.valueOf(body.get("selected").toString());

		cartService.updateSelected(userId, cartDetailId, selected);
	}

	@PostMapping("/update")
	@ResponseBody
	public void updateCart(@RequestBody Map<String, Integer> body, HttpSession session) {

		Integer userId = (Integer) session.getAttribute("USER_ID");

		cartService.updateCartItem(userId, body.get("cartDetailId"), body.get("quantity"));
	}

}
