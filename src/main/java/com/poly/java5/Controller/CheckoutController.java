package com.poly.java5.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.poly.java5.Bean.CheckoutBean;
import com.poly.java5.Entity.Order;
import com.poly.java5.Service.CartService;
import com.poly.java5.Service.CheckoutService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CheckoutController {
	 private final CartService cartService;
	    private final CheckoutService checkoutService;

	    // ================= HIỂN THỊ TRANG CHECKOUT =================
	    @GetMapping("/checkout")
	    public String checkoutPage(Model model, HttpSession session) {

	        Integer userId = (Integer) session.getAttribute("USER_ID");
	        if (userId == null) return "redirect:/login";

	        // 🔥 CHỈ LẤY SẢN PHẨM ĐÃ SELECT
	        List<Map<String, Object>> selectedItems =
	                cartService.getSelectedCartItems(userId);

	        model.addAttribute("cartDetails", selectedItems);


	        if (selectedItems.isEmpty()) {
	            // không có sản phẩm được chọn thì quay lại giỏ
	            return "redirect:/cart";
	        }

	        BigDecimal totalAmount =
	                cartService.getSelectedTotalAmount(userId);

	        BigDecimal shippingFee = BigDecimal.valueOf(15000);
	        BigDecimal discount = BigDecimal.ZERO;
	        BigDecimal finalAmount =
	                totalAmount.add(shippingFee).subtract(discount);

	        model.addAttribute("cartDetails", selectedItems);
	        model.addAttribute("totalAmount", totalAmount);
	        model.addAttribute("shippingFee", shippingFee);
	        model.addAttribute("discount", discount);
	        model.addAttribute("finalAmount", finalAmount);
	        model.addAttribute("checkoutBean", new CheckoutBean());

	        return "checkout";
	    }

	    // ================= XỬ LÝ ĐẶT HÀNG =================
	    @PostMapping("/checkout")
	    public String checkoutSubmit(
	            @Valid @ModelAttribute("checkoutBean") CheckoutBean bean,
	            BindingResult result,
	            Model model,
	            HttpSession session
	    ) {

	        Integer userId = (Integer) session.getAttribute("USER_ID");
	        if (userId == null) return "redirect:/login";

	        // Nếu validate lỗi → load lại sản phẩm đã select
	        if (result.hasErrors()) {

	        	List<Map<String, Object>> cartDetails =
	        	        cartService.getSelectedCartItems(userId);
	        	


	            BigDecimal totalAmount =
	                    cartService.getSelectedTotalAmount(userId);

	            BigDecimal shippingFee = BigDecimal.valueOf(15000);
	            BigDecimal discount = BigDecimal.ZERO;
	            BigDecimal finalAmount =
	                    totalAmount.add(shippingFee).subtract(discount);

	            model.addAttribute("cartDetails", cartDetails);
	            model.addAttribute("totalAmount", totalAmount);
	            model.addAttribute("shippingFee", shippingFee);
	            model.addAttribute("discount", discount);
	            model.addAttribute("finalAmount", finalAmount);

	            return "checkout";
	        }

	        // 🔥 CHECKOUT CHỈ DỰA TRÊN SELECTED
	        Order order = checkoutService.checkout(
	                userId,
	                bean.getCustomerName(),
	                bean.getCustomerPhone(),
	                bean.getCustomerAddress(),
	                bean.getPaymentMethod()
	        );

	        return "redirect:/order-success?code=" + order.getOrderCode();
	    }
}
