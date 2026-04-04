package com.poly.java5.Controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.poly.java5.Entity.Order;
import com.poly.java5.Service.OrderService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;


	 @GetMapping("/orders")
	 public String listOrders(Model model) {
	     List<Order> orders = orderService.findAll();
	     model.addAttribute("orders", orders);
	     return "orders"; 
	 }
	 
	 @GetMapping("/order-success")
		public String success(@RequestParam("code") String code, HttpSession session, Model model) {

			Integer userId = (Integer) session.getAttribute("USER_ID");
			if (userId == null)
				return "redirect:/login";

			Order order = orderService.findByCodeAndUser(code, userId);

			model.addAttribute("order", order);
			model.addAttribute("orderDetails", order.getOrderDetails());

			return "order-success";
		}


	// hiện chi tiết đơn hàng
	@GetMapping("/orders/detail")
	public String orderDetail(@RequestParam("code") String code, HttpSession session, Model model) {

		Integer userId = (Integer) session.getAttribute("USER_ID");
		if (userId == null)
			return "redirect:/login";

		Order order = orderService.findByCodeAndUser(code, userId);

		model.addAttribute("order", order);
		return "order-detail";
	}

	@GetMapping("/my-orders")
	public String myOrders(@RequestParam(required = false) String status, HttpSession session, Model model) {
		Integer userId = (Integer) session.getAttribute("USER_ID");
		if (userId == null)
			return "redirect:/login";

		List<Order> orders = orderService.findOrdersByUser(userId, status);

		model.addAttribute("orders", orders);
		model.addAttribute("currentStatus", status);

		return "order-list";
	}

	 //code sua
	@PostMapping("/orders/cancel/{id}")
	public String cancelOrder(@PathVariable("id") Integer id,
	                          HttpSession session) {

	    Integer userId = (Integer) session.getAttribute("USER_ID");
	    if (userId == null)
	        return "redirect:/login";

	    orderService.cancelOrder(id, userId);

	    return "redirect:/my-orders";
	}

}
