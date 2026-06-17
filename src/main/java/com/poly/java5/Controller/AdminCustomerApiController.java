package com.poly.java5.Controller;

import com.poly.java5.DTO.CustomerDTO;
import com.poly.java5.DTO.CustomerHistoryDTO;
import com.poly.java5.DTO.OrderDTO;
import com.poly.java5.DTO.ToggleStatusResponseDTO;
import com.poly.java5.Entity.User;
import com.poly.java5.Entity.UserRole;
import com.poly.java5.Service.UserService;
import com.poly.java5.Service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")

public class AdminCustomerApiController {

	@Autowired
	private UserService userService;

	@Autowired
	private OrderService orderService;

	@GetMapping
	public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
		List<User> users = userService.findByRole(UserRole.USER);
		List<CustomerDTO> result = new ArrayList<>();

		for (User u : users) {
			Double spending = orderService.sumSpendingByUsername(u.getUsername());
			if (spending == null)
				spending = 0.0;

			String type;
			if (spending >= 5_000_000) {
				type = "VIP (Thân thiết)";
			} else if (spending >= 1_000_000) {
				type = "Tiềm năng";
			} else {
				type = "Khách mới";
			}

			result.add(new CustomerDTO(u.getUsername(), u.getName(), u.getEmail(), u.getPhone(), u.getActive(),
					spending, type, u.getAvatar()));
		}
		return ResponseEntity.ok(result);
	}

	@GetMapping("/history/{username}")
	public ResponseEntity<CustomerHistoryDTO> getHistory(@PathVariable String username) {
		User user = userService.findByUsername(username);
		if (user == null)
			return ResponseEntity.notFound().build();

		Double spending = orderService.sumSpendingByUsername(username);
		List<OrderDTO> orders = orderService.findByUsername(username); // ✅ giờ là DTO

		return ResponseEntity.ok(new CustomerHistoryDTO(user.getUsername(), user.getName(), user.getEmail(),
				user.getPhone(), user.getActive(), spending != null ? spending : 0.0, orders, user.getAvatar()));
	}

	@PutMapping("/toggle/{username}")
	public ResponseEntity<ToggleStatusResponseDTO> toggleStatus(@PathVariable String username) {
		try {
			userService.toggleActive(username); // phương thức này sẽ đảo trạng thái và gửi mail
			User user = userService.findByUsername(username); // lấy lại để biết trạng thái mới
			String message = user.getActive() ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản";
			return ResponseEntity.ok(new ToggleStatusResponseDTO(message, user.getActive(), username));
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new ToggleStatusResponseDTO(e.getMessage(), false, username));
		}
	}
}